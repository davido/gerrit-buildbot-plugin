/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BuildbotVersion {
	private static final String version;

	public static String getVersion() {
		return version;
	}

	static {
		version = loadVersion();
	}

	private static String loadVersion() {
		InputStream in = BuildbotVersion.class.getResourceAsStream("Version");
		if (in == null) {
			return null;
		}
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(in,
					"UTF-8"));
			try {
				String vs = r.readLine();
				if (vs != null && vs.startsWith("v")) {
					vs = vs.substring(1);
				}
				if (vs != null && vs.isEmpty()) {
					vs = null;
				}
				return vs;
			} finally {
				r.close();
			}
		} catch (IOException e) {
			return null;
		}
	}

	private BuildbotVersion() {
	}
}
