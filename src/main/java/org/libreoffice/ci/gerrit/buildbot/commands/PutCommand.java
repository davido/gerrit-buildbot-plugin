/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.util.List;

import javax.annotation.Nullable;

import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
@CommandMetaData(name="get", descr="Acknowledge executed task and report the result")
public final class PutCommand extends BuildbotSshCommand {
	static final Logger log = LoggerFactory.getLogger(PutCommand.class);

	@Option(metaVar = "TICKET", name = "--ticket", aliases = { "-t" }, required = true, usage = "ticket of the job")
	private String ticket;

	@Option(name = "--id", aliases = { "-i" }, required = true, metaVar = "TB", usage = "id of the tinderbox")
	private String box;

	@Option(metaVar = "STATUS", name = "--status", aliases = { "-s" }, required = true, usage = "success|failed|canceled")
	private TaskStatus status;

	@Option(metaVar = "-|LOG", name = "--log", aliases = "-l", required = false, usage = "url of the job log page or - for standard input")
	private String urllog;

	@Inject
	@CanonicalWebUrl
	@Nullable
	Provider<String> urlProvider;

	@Inject IdentifiedUser user;

	@Override
	public void doRun() throws UnloggedFailure, OrmException, Failure {
	    synchronized (control) {
    		log.debug("ticket: {}", ticket);
    		if (Strings.isNullOrEmpty(ticket)) {
    			String tmp = "No ticket is provided";
    			stderr.print(tmp);
    			stderr.write("\n");
    			log.warn(tmp);
    			return;
    		}
            if (box != null) {
                String project = control.findProjectByTicket(ticket);
                if (project == null) {
                    String tmp = String.format("Can not find task for ticket %s",
                            ticket);
                    stderr.print(tmp);
                    stderr.write("\n");
                    log.warn(tmp);
                    return;
                }
                if (!config.isIdentityBuildbotAdmin4Project(project, user)) {
                    String message = String.format(
                            "only member of buildbot admin group allowed to pass --id option!",
                            project);
                    stderr.print(message);
                    stderr.write("\n");
                    return;
                }
            } else {
                // default is to use username as TB-ID
                box = user.getUserName();
            }
            if (status.isSuccess() || status.isFailed()) {
                if (Strings.isNullOrEmpty(urllog)) {
                    String tmp = String.format(
                            "No log is provided for status %s", status.name());
                    stderr.print(tmp);
                    stderr.write("\n");
                    log.warn(tmp);
                    return;
                }
            }
            if ("-".equals(urllog)) {
                urllog = config.getPublisher().publishLog(config, ticket,
                        box, status, in);
            }
    		TbJobResult result = control.setResultPossible(ticket, box, status,
    				urllog);
    		if (result == null) {
    			String tmp = String.format("Can not find task for ticket %s",
    					ticket);
    			stderr.print(tmp);
    			stderr.write("\n");
    			log.warn(tmp);
    			return;
    		}
    
    		final List<PatchSet> matches = db
    				.patchSets()
    				.byRevision(
    						new RevId(result.getTbPlatformJob()
    								.getParent().getGerritRevision())).toList();
    		if (matches.size() != 1) {
    			String tmp = String.format("Can not match patch set for revision %s",
    					result.getTbPlatformJob().getParent().getGerritRevision());
    			stderr.print(tmp);
    			stderr.write("\n");
    			log.warn(tmp);
    			return;
    		}
    		notifyGerritBuildbotPlatformJobFinished(result, matches.get(0));
    
    		try {
    			Thread.sleep(1000);
    		} catch(InterruptedException e) {
    			log.error("fatal: internal server error", e);
    			die(e);
    		}
    
    		if (result.getTbPlatformJob().getParent().allJobsReady()) {
    			notifyGerritJobFinished(result.getTbPlatformJob().getParent(), matches.get(0));
    		}
	    }
	}

	public void notifyGerritJobFinished(GerritJob job, final PatchSet ps) {
		StringBuilder builder = new StringBuilder(256);
		builder.append(String.format("Build %s:\n", job.getId()));
		short combinedStatus = 1;
		for (TbJobResult tbResult : job.getTbResultList()) {
			// ignore canceled tasks
			if (tbResult.ignoreJobStatus()) {
				continue;
			}
			if (!tbResult.getStatus().isSuccess()) {
				combinedStatus = -1;
			}
			// old not pulished impl on 2.5 branch was
			//short combinedStatus = verifiedType.getMax().getValue();
			//if (!tbResult.getStatus().isSuccess() || job.isStale()) {
            //    // Later we want this to be configurable
            //    // currently only report success and ignore failure
            //    // Note we reported already -1 on per platform task base
            //    log.warn("notifyGerritJobFinished return");
            //    return;
            //}
            builder.append(String.format("* on %s %s : %s\n",
                    tbResult.getPlatform()
                            .name(),
                    tbResult
                            .getStatus().name(),
                    Strings.nullToEmpty(tbResult.getLog())));
		}
		try {
			approveOne(ps.getId(), builder.toString(),
					"Verified", combinedStatus);
		} catch (Exception e) {
        	String tmp = String.format("fatal: internal server error while approving %s\n", ps.getId());
        	writeError(tmp);
        	log.error(tmp, e);
			die(e);
		}
	}

	void notifyGerritBuildbotPlatformJobFinished(TbJobResult tbJobResult, final PatchSet ps) {
		short status = 0;
		String msg = String.format("%s %s (%s)\n\nBuild on %s at %s: %s",
                tbJobResult.getPlatform().name(),
                tbJobResult.getStatus().name(),
                tbJobResult.getDecoratedId(),
                tbJobResult.getTinderboxId(),
                time(tbJobResult.getEndTime(), 0),
                Strings.nullToEmpty(tbJobResult.getLog()));
		try {
			approveOne(ps.getId(), msg, "Code-Review", status);
		} catch (Exception e) {
        	String tmp = String.format("fatal: internal server error while approving %s\n", ps.getId());
        	writeError(tmp);
			log.error(tmp, e);
			die(e);
		}
	}
}
