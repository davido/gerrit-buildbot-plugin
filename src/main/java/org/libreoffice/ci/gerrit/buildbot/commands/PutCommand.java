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
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritNotifyListener;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gerrit.common.data.ApprovalType;
import com.google.gerrit.common.data.ApprovalTypes;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.ApprovalCategory;
import com.google.gerrit.reviewdb.client.ApprovalCategoryValue;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.patch.PublishComments;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;
import com.google.inject.Provider;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class PutCommand extends SshCommand implements
        GerritNotifyListener {
    static final Logger log = LoggerFactory.getLogger(PutCommand.class);

    @Option(metaVar = "TICKET", name = "--ticket", aliases = { "-t" }, required = true, usage = "ticket of the job")
    private String ticket;

    @Option(name = "--id", aliases={"-i"}, required = true, metaVar = "TB", usage = "id of the tinderbox")
    private String boxId;
    
    @Option(metaVar = "STATUS", name = "--status", aliases = { "-s" }, required = true, usage = "success|failed|canceled")
    private TaskStatus status; 

    @Option(metaVar = "-|LOG", name = "--log", aliases = "-l", required = false, usage = "url of the job log page or - for standard input")
    private String urllog;

    @Inject
    BuildbotLogicControl control;

    @Inject
    private PublishComments.Factory publishCommentsFactory;

    @Inject
    BuildbotConfig config;

    private final static String LOGFILE_SUFFIX = ".log";

    @Inject
    private ApprovalTypes approvalTypes;

    @Inject
    private ReviewDb db;

    @Inject
    @CanonicalWebUrl
    @Nullable
    Provider<String> urlProvider;

    private final static String LOGFILE_SERVLET_SUFFIX = "plugins/buildbot/log?file=";

    protected String getDescription() {
        return "Acknowledge executed task and report the result";
    }

    @Override
    public void run() throws UnloggedFailure, Failure, Exception {
        log.debug("ticket: {}", ticket);
        if ("-".equals(urllog)) {
            writeLogFile();
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
        		String tmp = String.format("No log is provided for status %s", status.name());
        		stderr.print(tmp);
            	stderr.write("\n");
            	log.warn(tmp);
            	return;
        	}
        }
        
        TbJobResult result = control.setResultPossible(ticket, boxId, status, urllog);
        if (result == null) {
        	String tmp = String.format("Can not find task for ticket %s", ticket);
        	stderr.print(tmp);
        	stderr.write("\n");
        	log.warn(tmp);
        	return;
        }
        notifyGerritBuildbotPlatformJobFinished(result);
                
        // Synchronize?
        Thread.sleep(1000);

        synchronized (control) {
            if (result.getTbPlatformJob().getParent().allJobsReady()) {
                notifyGerritJobFinished(result.getTbPlatformJob().getParent());
            }
        }
    }

    private void writeLogFile() throws IOException {
        try {
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
        } catch (Exception e) {
            die(e);
        }
        urllog = urlProvider.get() + LOGFILE_SERVLET_SUFFIX + urllog;
    }

    @Override
    public void notifyGerritJobFinished(GerritJob job) {
        ApprovalCategory verified = null;
        for (ApprovalType type : approvalTypes.getApprovalTypes()) {
            final ApprovalCategory category = type.getCategory();
            if ("VRIF".equals(category.getId().get())) {
                verified = category;
                break;
            }
        }

        Set<ApprovalCategoryValue.Id> aps = new HashSet<ApprovalCategoryValue.Id>();
        PatchSet patchset = null;
        try {
            ResultSet<PatchSet> result = db.patchSets().byRevision(
                    new RevId(job.getGerritRevision()));
            patchset = result.iterator().next();
            // think positive ;-)
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
                builder.append(String.format("* Build %s on %s completed %s : %s\n",
                        tbResult.getDecoratedId(),
                        tbResult.getPlatform().name(),
                        tbResult.getLog() == null ? StringUtils.EMPTY : tbResult.getLog(),
                        tbResult.getStatus().name()));
            }
            aps.add(new ApprovalCategoryValue.Id(verified.getId(), combinedStatus));            
            getCommenter(aps, patchset, builder).call();
        } catch (Exception e) {
            e.printStackTrace();
            die(e);
        }
    }

	private PublishComments getCommenter(
			Set<ApprovalCategoryValue.Id> aps, PatchSet patchset,
			StringBuilder builder) throws NoSuchFieldException,
			IllegalAccessException {
		PublishComments commenter = publishCommentsFactory.create(patchset.getId(),
		        builder.toString(), aps, true);
		if (config.isForgeReviewerIdentity()) {
			// Replace current user with buildbot user
			Field field = commenter.getClass().getDeclaredField("user");
			field.setAccessible(true);
			field.set(commenter, control.getBuildbot());
		}
		return commenter;
	}

    void notifyGerritBuildbotPlatformJobFinished(TbJobResult tbJobResult) {
        ApprovalCategory verified = null;
        for (ApprovalType type : approvalTypes.getApprovalTypes()) {
            final ApprovalCategory category = type.getCategory();
            // VRIF
            if ("CRVW".equals(category.getId().get())) {
                verified = category;
                break;
            }
        }

        Set<ApprovalCategoryValue.Id> aps = new HashSet<ApprovalCategoryValue.Id>();
        PatchSet patchset = null;
        try {
            ResultSet<PatchSet> result = db.patchSets().byRevision(
                    new RevId(tbJobResult.getTbPlatformJob().getParent().getGerritRevision()));
            patchset = result.iterator().next();
            StringBuilder builder = new StringBuilder(256);
            // we don't know what other guys say...
            short status = 0;
            builder.append(String.format("Build %s on %s complete at %s %s : %s",
                    tbJobResult.getDecoratedId(),
                    tbJobResult.getPlatform().name(),
                    time(tbJobResult.getEndTime(), 0),
                    tbJobResult.getLog() == null ? StringUtils.EMPTY : tbJobResult.getLog(),
                    tbJobResult.getStatus().name()));
            aps.add(new ApprovalCategoryValue.Id(verified.getId(), status));            
            getCommenter(aps, patchset, builder).call();
        } catch (Exception e) {
            e.printStackTrace();
            die(e);
        }
    }

    private static String time(final long now, final long delay) {
        final Date when = new Date(now + delay);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }
}
