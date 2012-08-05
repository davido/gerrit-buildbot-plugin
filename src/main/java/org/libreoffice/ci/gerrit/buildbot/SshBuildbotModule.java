/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot;

import org.libreoffice.ci.gerrit.buildbot.commands.GetTaskCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.ReportCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.ShowQueueCommand;

import com.google.gerrit.sshd.PluginCommandModule;

public class SshBuildbotModule extends PluginCommandModule {

	@Override
	protected void configureCommands() {
		command("show-queue").to(ShowQueueCommand.class);
		command("get-task").to(GetTaskCommand.class);
		command("report").to(ReportCommand.class);
	}

}
