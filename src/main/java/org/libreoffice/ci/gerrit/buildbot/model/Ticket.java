/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.model;

import com.google.gerrit.server.util.IdGenerator;

public class Ticket {
	int id;
	Platform tbPlatform;
	long startTime;

	public Ticket(int id, Platform tbPlatform) {
		this.id = id;
		this.tbPlatform = tbPlatform;
		startTime = System.currentTimeMillis();
	}

	@Override
	public String toString() {

		return id(id) + "_" + tbPlatform.name();
	}

	public long getStartTime() {
		return startTime;
	}
	

	private static String id(final int id) {
		  return IdGenerator.format(id);
	}
	
	public String getPlatform() {
		return tbPlatform.toString();
	}

	public String getDecoratedId() {
		return id(id);
	}
}
