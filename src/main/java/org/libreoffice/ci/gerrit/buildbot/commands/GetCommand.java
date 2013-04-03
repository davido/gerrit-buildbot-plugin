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
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gwtorm.server.OrmException;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class GetCommand extends BuildbotSshCommand {
    static final Logger log = LoggerFactory.getLogger(GetCommand.class);

    @Option(name = "--project", aliases={"-p"}, required = true, metaVar = "PROJECT", usage = "name of the project for which the task should be polled")
    private ProjectControl projectControl;

    @Option(name = "--platform", aliases={"-a"}, required = true, metaVar = "PLATFORM", usage = "name of the platform")
    private Platform platform;
    
    @Option(name = "--id", aliases={"-i"}, required = true, metaVar = "TB", usage = "id of the tinderbox")
    private String box;

    @Option(name = "--format", aliases={"-f"}, required = false, metaVar = "FORMAT", usage = "output display format")
    private FormatType format = FormatType.TEXT;
    
	@Argument(index = 0, required = false, multiValued = true, metaVar = "BRANCH", usage = "branch[es] to get a task for")
	void addPatchSetId(final String branch) {
		branchSet.add(branch);
	}
	
	private Set<String> branchSet = Sets.newHashSet();

	protected String getDescription() {
		return "Get a task from platform specific queue";
	}

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
    		TbJobDescriptor jobDescriptor = control.launchTbJob(projectControl
    				.getProject().getName(), platform, branchSet, box);
            if (jobDescriptor == null) {
                if (format != null && format == FormatType.BASH) {
                    stdout.print(String.format("GERRIT_TASK_TICKET=\nGERRIT_TASK_BRANCH=\nGERRIT_TASK_REF=\n"));
                } else {
                    stdout.print("empty");
                }
            } else {	
    			final List<PatchSet> matches = db
    					.patchSets()
    					.byRevision(
    							new RevId(jobDescriptor.getBuildbotPlatformJob()
    									.getParent().getGerritRevision())).toList();
    			if (matches.size() == 1) {
    				notifyGerritBuildbotPlatformJobStarted(jobDescriptor.getBuildbotPlatformJob(),
    						matches.get(0));
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

    void notifyGerritBuildbotPlatformJobStarted(final BuildbotPlatformJob tbPlatformJob,
    		final PatchSet ps) {
        final short status = 0;
        String changeComment = String.format("Build %s on %s started at %s by %s\n\n",
                tbPlatformJob.getTicket().getId(),
                tbPlatformJob.getPlatformString(),
                time(tbPlatformJob.getStartTime(), 0),
                tbPlatformJob.getTinderboxId());            
        try {
            approveOne(ps.getId(), changeComment, "Code-Review", status);
        } catch (Exception e) {
        	String tmp = String.format("fatal: internal server error while approving %s\n", ps.getId());
        	writeError(tmp);
        	log.error(tmp, e);
            die(e);
        }
    }
}
