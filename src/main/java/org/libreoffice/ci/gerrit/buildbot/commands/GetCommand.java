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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gerrit.common.data.ApprovalType;
import com.google.gerrit.common.data.ApprovalTypes;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.ApprovalCategory;
import com.google.gerrit.reviewdb.client.ApprovalCategoryValue;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.patch.PublishComments;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class GetCommand extends SshCommand {
    static final Logger log = LoggerFactory.getLogger(GetCommand.class);

    @Option(name = "--project", aliases = { "-p" }, required = true, metaVar = "PROJECT", usage = "name of the project for which the task should be polled")
    private ProjectControl projectControl;

    @Option(name = "--platform", aliases = { "-a" }, required = true, metaVar = "PLATFORM", usage = "name of the platform")
    private Platform platform;

    @Option(name = "--id", aliases = { "-i" }, required = true, metaVar = "TB", usage = "id of the tinderbox")
    private String box;

    @Option(name = "--format", aliases = { "-f" }, required = false, metaVar = "FORMAT", usage = "output display format")
    private FormatType format = FormatType.TEXT;

    @Argument(index = 0, required = false, multiValued = true, metaVar = "BRANCH", usage = "branch[es] to get a task for")
    void addPatchSetId(final String branch) {
        branchSet.add(branch);
    }

    @Option(name = "--test", aliases = { "-t" }, required = false, metaVar = "TEST", usage = "peek a task for test only. Task is not removed from the queue and no reporting a result is possible.")
    boolean test = false;

    private Set<String> branchSet = Sets.newHashSet();

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

    protected String getDescription() {
        return "Get a task from platform specific queue";
    }

    @Override
    public void run() throws UnloggedFailure, Failure, Exception {
        synchronized (control) {
            log.debug("project: {}", projectControl.getProject().getName());
            if (!config.isProjectSupported(projectControl.getProject()
                    .getName())) {
                String message = String.format(
                        "project <%s> is not enabled for building!",
                        projectControl.getProject().getName());
                stderr.print(message);
                stderr.write("\n");
                return;
            }
            TbJobDescriptor jobDescriptor = control.launchTbJob(projectControl
                    .getProject().getName(), platform, branchSet, box, test);
            if (jobDescriptor == null) {
                if (format != null && format == FormatType.BASH) {
                    stdout.print(String
                            .format("GERRIT_TASK_TICKET=\nGERRIT_TASK_BRANCH=\nGERRIT_TASK_REF=\n"));
                } else {
                    stdout.print("empty");
                }
            } else {
                if (!test) {
                    notifyGerritBuildbotPlatformJobStarted(jobDescriptor
                            .getBuildbotPlatformJob());
                }
                String output;
                if (format != null && format == FormatType.BASH) {
                    output = String
                            .format("GERRIT_TASK_TICKET=%s\nGERRIT_TASK_BRANCH=%s\nGERRIT_TASK_REF=%s\n",
                                    jobDescriptor.getTicket(),
                                    jobDescriptor.getBranch(),
                                    jobDescriptor.getRef());
                } else {
                    output = String.format(
                            "engaged: ticket=%s branch=%s ref=%s\n",
                            jobDescriptor.getTicket(),
                            jobDescriptor.getBranch(), jobDescriptor.getRef());
                }
                stdout.print(output);
            }
        }
    }

    void notifyGerritBuildbotPlatformJobStarted(
            BuildbotPlatformJob tbPlatformJob) {
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
                    new RevId(tbPlatformJob.getParent().getGerritRevision()));
            patchset = result.iterator().next();
            StringBuilder builder = new StringBuilder(256);
            short status = 0;
            builder.append(String.format(
                    "Build %s on %s started by TB %s at %s\n\n", tbPlatformJob
                            .getTicket().getId(), tbPlatformJob
                            .getPlatformString(), tbPlatformJob
                            .getTinderboxId(),
                    time(tbPlatformJob.getStartTime(), 0)));
            aps.add(new ApprovalCategoryValue.Id(verified.getId(), status));
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

    private static String time(final long now, final long delay) {
        final Date when = new Date(now + delay);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }
}
