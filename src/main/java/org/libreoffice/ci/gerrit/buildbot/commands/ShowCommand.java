/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.logic.LogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class ShowCommand extends SshCommand {
    static final Logger log = LoggerFactory.getLogger(ShowCommand.class);

    @Option(name = "--project", aliases = { "-p" }, required = true, metaVar = "PROJECT", usage = "name of the project for which the queue should be shown")
    private ProjectControl projectControl;

    @Option(name = "--type", aliases = { "-t" }, required = false, metaVar = "TYPE", usage = "which type of tasks to display")
    private TaskType type;

    @Inject
    LogicControl control;

    public ShowCommand() {
        log.debug("in ctr");
    }

    @Override
    public void run() throws UnloggedFailure, Failure, Exception {
        log.debug("project: {}", projectControl.getProject().getName());

        if (!control.isProjectSupported(projectControl.getProject().getName())) {
            String message = String.format(
                    "project <%s> is not enabled for building!", projectControl
                            .getProject().getName());
            stderr.print(message);
            stderr.write("\n");
            return;
        }

        stdout.print("----------------------------------------------"
                + "--------------------------------\n");

        stdout.print(String.format("%-17s %-12s %-12s %-22s %-3s %s\n", //
                "Task-Id", "Start/End", "Type/State", "Ref", "Bot", "Branch"));
        int numberOfPendingTasks = 0;

        List<GerritJob> changes = control.getGerritJobs();
        synchronized (changes) {
            for (GerritJob change : changes) {
                if (type == null || type.equals(TaskType.CHANGE)) {
                    numberOfPendingTasks++;
                    stdout.print(String.format(
                            "%-17s %-12s %-12s %-22s %-3s %s\n", //
                            change.getId(), time(change.getStartTime(), 0),
                            "Change", change.getGerritRef(),
                            "-",
                            change.getGerritBranch()));
                }
                if (type == null || type.equals(TaskType.JOB)) {
                    List<BuildbotPlatformJob> list = change.getBuildbotList();
                    synchronized (list) {
                        for (BuildbotPlatformJob job : list) {
                            String jobId = job.getParent().getId() + "_"
                                    + job.getPlatformString();
                            String time = "-";
                            Ticket t = job.getTicket();
                            String status = "Job: ";
                            if (job.getResult() != null && job.getResult().getStatus().isDiscarded()) {
                            	status += "DISCARDED";
                            } else if (!job.isStarted()) {
                                status += "INIT";
                            } else if (job.isReady()) {
                                status += job.getResult().getStatus().name();
                                time = time(job.getResult().getEndTime(), 0);
                            } else {
                                status += "STARTED";
                                time = time(t.getStartTime(), 0);
                            }
                            stdout.print(String.format(
                                    "%-17s %-12s %-12s %-22s %-3s %s\n", //
                                    jobId, time, status, job.getParent()
                                            .getGerritRef(),
                                            job.getTinderboxId() == null ? "-" : job.getTinderboxId(),
                                            job.getParent()
                                            .getGerritBranch()));
                            numberOfPendingTasks++;
                        }
                    }
                }
            }
        }
        stdout.print("----------------------------------------------"
                + "--------------------------------\n");
        stdout.print("  " + numberOfPendingTasks + " task(s)\n");
    }

    private static String time(final long now, final long delay) {
        final Date when = new Date(now + delay);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }
}
