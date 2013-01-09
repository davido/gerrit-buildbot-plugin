/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.model;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;

public class TbJobResult {
	BuildbotPlatformJob tbPlatformJob;
	String decoratedId;
	Platform platform;
	TaskStatus status;
	String log;
	long endTime;

	public TbJobResult(BuildbotPlatformJob tbPlatformJob, String decoratedId, 
			Platform platform, TaskStatus status, String log) {
		this.tbPlatformJob = tbPlatformJob;
		this.decoratedId = decoratedId;
		this.platform = platform;
		this.status = status;
		this.log = log;
		this.endTime = System.currentTimeMillis();
	}
	
	public String getDecoratedId() {
		return decoratedId;
	}
	
	public String getLog() {
		return log;
	}

	public Platform getPlatform() {
		return platform;
	}

	public TaskStatus getStatus() {
		return status;
	}
	
	public long getEndTime() {
		return endTime;
	}
	
	public BuildbotPlatformJob getTbPlatformJob() {
		return tbPlatformJob;
	}

	public boolean ignoreJobStatus() {
		if (getStatus() == TaskStatus.CANCELED) {
			return true;
		}
		return false;
	}	
}
