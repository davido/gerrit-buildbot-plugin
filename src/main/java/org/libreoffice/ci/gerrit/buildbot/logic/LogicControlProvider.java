/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.logic;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.impl.LogicControlImpl;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LogicControlProvider implements Provider<LogicControl> {

	BuildbotConfig config;

	@Inject
	LogicControlProvider(BuildbotConfig config) {
		this.config = config;
	}

	public LogicControl get() {
		return new LogicControlImpl(config);
	}
}
