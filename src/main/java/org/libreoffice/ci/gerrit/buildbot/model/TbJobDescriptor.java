/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.model;

public class TbJobDescriptor {
	BuildbotPlatformJob buildbotPlatformJob;
	String ticket;
	String branch;
	String ref; 

	public TbJobDescriptor(BuildbotPlatformJob job) {
		this.buildbotPlatformJob = job;
		this.ticket = job.getTicketString();
		this.branch = job.getParent().getGerritBranch();
		this.ref = job.getParent().getGerritRef();
	}

	public String getBranch() {
		return branch;
	}

	public String getTicket() {
		return ticket;
	}
	
	public String getRef() {
		return ref;
	}
	
	public BuildbotPlatformJob getBuildbotPlatformJob() {
		return buildbotPlatformJob;
	}
}
