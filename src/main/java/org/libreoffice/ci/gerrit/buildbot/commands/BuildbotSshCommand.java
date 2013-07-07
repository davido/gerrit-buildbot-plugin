/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;

import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

public abstract class BuildbotSshCommand extends SshCommand {

	@Inject
	protected BuildbotLogicControl control;

	@Inject
	protected BuildbotConfig config;
    
	protected abstract void doRun() throws UnloggedFailure, OrmException, Failure;

	@Override
    protected final void run() throws UnloggedFailure, OrmException, Failure {
	    doRun();
	}

    protected static String time(final long now, final long delay) {
        final Date when = new Date(now + delay);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }

	protected void writeError(final String msg) {
		try {
			err.write(msg.getBytes(ENC));
		} catch (IOException e) {}
	}

}
