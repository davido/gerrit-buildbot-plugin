/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot;

import org.libreoffice.ci.gerrit.buildbot.commands.GetCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.PutCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.ScheduleCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.ShowCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.TestLogChannelCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.VerifyCommand;
import org.libreoffice.ci.gerrit.buildbot.commands.VersionCommand;

import com.google.gerrit.sshd.PluginCommandModule;

public class SshBuildbotModule extends PluginCommandModule {
	
	@Override
	protected void configureCommands() {
		command(ShowCommand.class);
		command(GetCommand.class);
		command(PutCommand.class);
		command(ScheduleCommand.class);
		command(TestLogChannelCommand.class);
		command(VerifyCommand.class);
		command(VersionCommand.class);
	}

}
