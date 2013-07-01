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
import org.libreoffice.ci.gerrit.buildbot.utils.QueueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.inject.Inject;
import com.google.inject.Provider;

@CommandMetaData(name="show", descr="Display the buildbot work queue")
public final class ShowCommand extends BuildbotSshCommand {
    private static final Logger log = LoggerFactory.getLogger(ShowCommand.class);

    @Option(name = "--project", aliases = { "-p" }, required = true, metaVar = "PROJECT", usage = "name of the project for which the queue should be shown")
    private ProjectControl projectControl;

    @Option(name = "--type", aliases = { "-t" }, required = false, metaVar = "TYPE", usage = "which type of tasks to display")
    private TaskType type;

    @Option(name = "--dump", aliases = { "-d" }, required = false, metaVar = "DUMP", usage = "dump all platform queues")
    private boolean dump = false;

    @Inject
    private Provider<CurrentUser> cu;

    @Override
    public void doRun() {
        synchronized (control) {
            final String p = projectControl.getProject().getName();
            log.debug("project: {}", p);
            if (!config.isProjectSupported(p)) {
                String message = String.format(
                        "project <%s> is not enabled for building!", projectControl
                                .getProject().getName());
                stderr.print(message);
                stderr.write("\n");
                return;
            }
            if (!cu.get().getEffectiveGroups()
                    .contains(config.findProject(p).getBuildbotAdminGroupId())
                    && 
                    !cu.get().getEffectiveGroups()
                    .contains(config.findProject(p).getBuildbotUserGroupId())) {
                String tmp = String.format(
                        "error: %s has not the ACL to call show command",
                        Objects.firstNonNull(cu.get().getUserName(), "n/a"));
                log.warn(tmp);
                stderr.print(tmp + "\n");
                return;
            }
            QueueUtils.dumpQueue(stdout, type, control, p, dump);
        }
    }
}
