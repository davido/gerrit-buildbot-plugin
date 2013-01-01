/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.config;

public class BuildbotConfig {

	private String email;
	private String project;
	private TriggerStrategie triggerStrategie;
	private String reviewerGroupName;
	private String logDir;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getLogDir() {
		return logDir;
	}

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	public void setTriggerStrategie(TriggerStrategie triggerStrategie) {
		this.triggerStrategie = triggerStrategie;		
	}
	
	public TriggerStrategie getTriggerStrategie() {
		return this.triggerStrategie;
	}

	public String getReviewerGroupName() {
		return reviewerGroupName;
	}

	public void setReviewerGroupName(String reviewerGroupName) {
		this.reviewerGroupName = reviewerGroupName;
	}
}
