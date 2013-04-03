/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
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
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class PutCommand extends BuildbotSshCommand {
	static final Logger log = LoggerFactory.getLogger(PutCommand.class);

	@Option(metaVar = "TICKET", name = "--ticket", aliases = { "-t" }, required = true, usage = "ticket of the job")
	private String ticket;

	@Option(name = "--id", aliases = { "-i" }, required = true, metaVar = "TB", usage = "id of the tinderbox")
	private String boxId;

	@Option(metaVar = "STATUS", name = "--status", aliases = { "-s" }, required = true, usage = "success|failed|canceled")
	private TaskStatus status;

	@Option(metaVar = "-|LOG", name = "--log", aliases = "-l", required = false, usage = "url of the job log page or - for standard input")
	private String urllog;

	@Inject
	@CanonicalWebUrl
	@Nullable
	Provider<String> urlProvider;

	private final static String LOGFILE_SERVLET_SUFFIX = "plugins/buildbot/log?file=";
	private final static String LOGFILE_SUFFIX = ".log";

	protected String getDescription() {
		return "Acknowledge executed task and report the result";
	}

	@Override
	public void doRun() throws UnloggedFailure, OrmException, Failure {
	    synchronized (control) {
    		log.debug("ticket: {}", ticket);
    		if ("-".equals(urllog)) {
    			try {
    				writeLogFile();
    			} catch (IOException e) {
    				log.error(e.getMessage());
    				die(e);
    			}
    		}
    
    		if (Strings.isNullOrEmpty(ticket)) {
    			String tmp = "No ticket is provided";
    			stderr.print(tmp);
    			stderr.write("\n");
    			log.warn(tmp);
    			return;
    		}
    
    		if (status.isSuccess() || status.isFailed()) {
    			if (Strings.isNullOrEmpty(urllog)) {
    				String tmp = String.format("No log is provided for status %s",
    						status.name());
    				stderr.print(tmp);
    				stderr.write("\n");
    				log.warn(tmp);
    				return;
    			}
    		}
    
    		TbJobResult result = control.setResultPossible(ticket, boxId, status,
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
    		    log.debug("all jobs are ready");
    			notifyGerritJobFinished(result.getTbPlatformJob().getParent(), matches.get(0));
    		}
    		
    		log.debug("done");
	    }
	}

	public void notifyGerritJobFinished(GerritJob job, final PatchSet ps) {
		StringBuilder builder = new StringBuilder(256);
		short combinedStatus = 1;
		for (TbJobResult tbResult : job.getTbResultList()) {
			// ignore canceled tasks
			if (tbResult.ignoreJobStatus()) {
				continue;
			}
			if (!tbResult.getStatus().isSuccess()) {
				combinedStatus = -1;
			}
			builder.append(String.format(
					"* Build %s on %s completed %s : %s\n",
					tbResult.getDecoratedId(),
					tbResult.getPlatform().name(),
					tbResult.getLog() == null ? StringUtils.EMPTY : tbResult
							.getLog(), tbResult.getStatus().name()));
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
		String msg = String.format(
				"Build %s on %s complete at %s %s : %s", tbJobResult
						.getDecoratedId(),
				tbJobResult.getPlatform().name(),
				time(tbJobResult.getEndTime(), 0),
				tbJobResult.getLog() == null ? StringUtils.EMPTY
						: tbJobResult.getLog(), tbJobResult.getStatus()
						.name());
		try {
			approveOne(ps.getId(), msg, "Code-Review", status);
		} catch (Exception e) {
        	String tmp = String.format("fatal: internal server error while approving %s\n", ps.getId());
        	writeError(tmp);
			log.error(tmp, e);
			die(e);
		}
	}

	private void writeLogFile() throws IOException {
		urllog = ticket + LOGFILE_SUFFIX;
		String file = config.getLogDir() + File.separator + urllog;
		int sChunk = 8192;
		GZIPInputStream zipin = new GZIPInputStream(in);
		byte[] buffer = new byte[sChunk];
		FileOutputStream out = new FileOutputStream(file);
		int length;
		while ((length = zipin.read(buffer, 0, sChunk)) != -1)
			out.write(buffer, 0, length);
		out.flush();
		out.close();
		zipin.close();
		in.close();		
		urllog = urlProvider.get() + LOGFILE_SERVLET_SUFFIX + urllog;
	}

}
