/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
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
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.patch.PublishComments;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class PutCommand extends SshCommand {
    static final Logger log = LoggerFactory.getLogger(PutCommand.class);

    @Option(metaVar = "TICKET", name = "--ticket", aliases = { "-t" }, required = true, usage = "ticket of the job")
    private String ticket;

    @Option(name = "--id", aliases={"-i"}, required = false, metaVar = "TB", usage = "id of the tinderbox")
    private String box;
    
    @Option(metaVar = "STATUS", name = "--status", aliases = { "-s" }, required = true, usage = "success|failed|canceled|cancelled")
    private TaskStatus status; 

    @Option(metaVar = "-|LOG", name = "--log", aliases = "-l", required = false, usage = "url of the job log page or - for standard input")
    private String urllog;

    @Inject
    BuildbotLogicControl control;

    @Inject
    private PublishComments.Factory publishCommentsFactory;

    @Inject
    BuildbotConfig config;

    @Inject
    private ApprovalTypes approvalTypes;

    @Inject
    private ReviewDb db;

    @Inject IdentifiedUser user;

    protected String getDescription() {
        return "Acknowledge executed task and report the result";
    }

    @Override
    public void run() throws UnloggedFailure, Failure, Exception {
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
            TbJobResult result = control.setResultPossible(ticket, box,
                    status, urllog);
            if (result == null) {
                String tmp = String.format("Can not find task for ticket %s",
                        ticket);
                stderr.print(tmp);
                stderr.write("\n");
                log.warn(tmp);
                return;
            }
            notifyGerritBuildbotPlatformJobFinished(result);
            // Synchronize?
            Thread.sleep(1000);
            if (result.getTbPlatformJob().getParent().allJobsReady()) {
                notifyGerritJobFinished(result.getTbPlatformJob().getParent());
            }
        }
    }

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
            if (job.isStale()) {
                builder.append("Stale patch set: ignore verification status\n\n");
            }
            for (TbJobResult tbResult : job.getTbResultList()) {
                // ignore canceled tasks
                if (tbResult.ignoreJobStatus()) {
                    continue;
                }
                if (!tbResult.getStatus().isSuccess()) {
                    combinedStatus = -1;
                }
                builder.append(String.format("* Build %s on %s %s : %s\n",
                        tbResult.getDecoratedId(), tbResult.getPlatform()
                                .name(),
                        Strings.nullToEmpty(tbResult.getLog()), tbResult
                                .getStatus().name()));
            }
            if (!job.isStale()) {
                aps.add(new ApprovalCategoryValue.Id(verified.getId(), combinedStatus));
            }
            getCommenter(aps, patchset, builder).call();
        } catch (Exception e) {
            e.printStackTrace();
            die(e);
        }
    }

    private PublishComments getCommenter(Set<ApprovalCategoryValue.Id> aps,
            PatchSet patchset, StringBuilder builder)
            throws NoSuchFieldException, IllegalAccessException {
        PublishComments commenter = publishCommentsFactory.create(
                patchset.getId(), builder.toString(), aps, true);
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
            builder.append(String.format("Build %s on %s by TB %s at %s %s : %s",
                    tbJobResult.getDecoratedId(),
                    tbJobResult.getPlatform().name(),
                    tbJobResult.getTinderboxId(),
                    time(tbJobResult.getEndTime(), 0),
                    Strings.nullToEmpty(tbJobResult.getLog()),
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
