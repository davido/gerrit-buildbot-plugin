/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.utils.QueueUtils;
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

    @Option(name = "--dump", aliases = { "-d" }, required = false, metaVar = "DUMP", usage = "dump all platform queues")
    private boolean dump = false;

    @Inject
    BuildbotLogicControl control;

    @Inject
    BuildbotConfig config;

    protected String getDescription() {
        return "Display the buildbot work queue";
    }

    @Override
    public void run() throws UnloggedFailure, Failure, Exception {
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
            QueueUtils.dumpQueue(stdout, type, control, projectControl.getProject().getName(), dump);
        }
    }
}
