/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
@CommandMetaData(name="test-log-channel", descr="Test extern log channel")
public final class TestLogChannelCommand extends SshCommand {
    static final Logger log = LoggerFactory.getLogger(TestLogChannelCommand.class);

    @Inject
    BuildbotConfig config;

    @Override
    public void run() throws UnloggedFailure, Failure, Exception {
        if (!config.isExternalLogViewer()) {
            String tmp = "No external log channel configured";
            stderr.print(tmp);
            stderr.write("\n");
            log.warn(tmp);
            return;
        }
        String testChannel = config.getPublisher().testChannel(config);
        stderr.print(testChannel);
    }
}
