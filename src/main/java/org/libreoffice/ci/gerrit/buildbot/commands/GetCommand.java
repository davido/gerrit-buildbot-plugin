/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.Os;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.review.ReviewPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
@CommandMetaData(name="get", descr="Get a task from platform specific queue")
public final class GetCommand extends BuildbotSshCommand {
    static final Logger log = LoggerFactory.getLogger(GetCommand.class);

    @Option(name = "--project", aliases = { "-p" }, required = true, metaVar = "PROJECT", usage = "name of the project for which the task should be polled")
    private ProjectControl projectControl;

    @Option(name = "--platform", aliases = { "-a" }, required = false, metaVar = "PLATFORM", usage = "name of the platform (deprecated, use --os instead)")
    private Platform platform;
    
    @Option(name = "--os", aliases = { "-o" }, required = false, metaVar = "OS", usage = "name of the operating system")
    private Os os;

    @Option(name = "--id", aliases = { "-i" }, required = false, metaVar = "TB", usage = "id of the tinderbox")
    private String box;

    @Option(name = "--format", aliases = { "-f" }, required = false, metaVar = "FORMAT", usage = "output display format")
    private FormatType format = FormatType.TEXT;

    @Argument(index = 0, required = false, multiValued = true, metaVar = "BRANCH", usage = "branch[es] to get a task for")
    void addPatchSetId(final String branch) {
        branchSet.add(branch);
    }

    @Option(name = "--test", aliases = { "-t" }, required = false, metaVar = "TEST", usage = "peek a task for test only. Task is not removed from the queue and no reporting a result is possible.")
    boolean test = false;

    @Inject
    private IdentifiedUser user;

    @Inject
    private ReviewPublisher publisher;

    private Set<String> branchSet = Sets.newHashSet();

    @Override
    public void doRun() throws UnloggedFailure, OrmException, Failure {
        synchronized (control) {
            log.debug("project: {}", projectControl.getProject().getName());
            if (!config.isProjectSupported(projectControl.getProject().getName())) {
                String message = String.format(
                        "project <%s> is not enabled for building!", projectControl
                                .getProject().getName());
                stderr.print(message);
                stderr.write("\n");
                return;
            }
            if (os == null && platform == null) {
                stderr.print("os or platorm parameter must be not empty!");
                stderr.write("\n");
                return;
            }
            if (platform != null) {
                os = platform.toOs();
            }
            if (box != null) {
                if (!config.isIdentityBuildbotAdmin4Project(projectControl
                        .getProject().getName(), user)) {
                    String message = String.format(
                            "only member of buildbot admin group allowed to pass --id option!",
                            projectControl.getProject().getName());
                    stderr.print(message);
                    stderr.write("\n");
                    return;
                }
            } else {
                // default is to use username as TB-ID
                box = user.getUserName();
            }
    		TbJobDescriptor jobDescriptor = control.launchTbJob(projectControl
    				.getProject().getName(), os, branchSet, box, test);
            if (jobDescriptor == null) {
                if (format != null && format == FormatType.BASH) {
                    stdout.print(String.format("GERRIT_TASK_TICKET=\nGERRIT_TASK_BRANCH=\nGERRIT_TASK_REF=\n"));
                } else {
                    stdout.print("empty");
                }
            } else {
                if (!test) {
        			notifyGerritBuildbotPlatformJobStarted(jobDescriptor.getBuildbotPlatformJob());
                }
                reportOutcome(jobDescriptor);
            }
        }
    }

	private void reportOutcome(TbJobDescriptor jobDescriptor) {
		String output;
		if (format != null && format == FormatType.BASH) {
		    output = String.format("GERRIT_TASK_TICKET=%s\nGERRIT_TASK_BRANCH=%s\nGERRIT_TASK_REF=%s\n",
		            jobDescriptor.getTicket(),
		            jobDescriptor.getBranch(),
		            jobDescriptor.getRef());
		} else {
		    output = String.format("engaged: ticket=%s branch=%s ref=%s\n",
		            jobDescriptor.getTicket(),
		            jobDescriptor.getBranch(),
		            jobDescriptor.getRef());
		}
		stdout.print(output);
	}

    void notifyGerritBuildbotPlatformJobStarted(final BuildbotPlatformJob job) {
        final short status = 0;
        String changeComment = String.format(
                "%s build started for %s on %s at %s\n\n", job
                        .getPlatformString(), job
                        .getTicket().getId(), job
                        .getTinderboxId(),
                time(job.getStartTime(), 0));
        try {
            publisher.approveOne(job.getParent(), changeComment, "Code-Review", status);
        } catch (Exception e) {
        	String tmp = String.format("fatal: internal server error while approving\n");
        	writeError(tmp);
        	log.error(tmp, e);
            die(e);
        }
    }
}
