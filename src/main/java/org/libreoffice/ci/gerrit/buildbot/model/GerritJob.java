/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.libreoffice.ci.gerrit.buildbot.logic.impl.LogicControlImpl;

public class GerritJob implements Runnable {
	String gerritBranch;
	String gerritRef;
	String gerritRevision;
	Thread thread;
	int id;
	long startTime;

	final List<BuildbotPlatformJob> tinderBoxThreadList = Collections
			.synchronizedList(new ArrayList<BuildbotPlatformJob>());
	List<TbJobResult> tbResultList;
	LogicControlImpl control;

	public GerritJob(LogicControlImpl control, String gerritBranch,
			String gerritRef, String gerritRevision, int id) {
		this.control = control;
		this.gerritBranch = gerritBranch;
		this.gerritRef = gerritRef;
		this.gerritRevision = gerritRevision;
		this.id = id;
		this.startTime = System.currentTimeMillis();
	}

	public void start() {
		thread = new Thread(this, "name");
		thread.start();
	}

	public String getGerritBranch() {
		return gerritBranch;
	}

	public boolean allJobsReady() {
		boolean done = true;
		for (BuildbotPlatformJob tbJob : tinderBoxThreadList) {
			if (!tbJob.isReady()) {
				done = false;
			}
		}
		return done;
	}

	@Override
	public void run() {

		boolean done = false;
		while (!done) {
			done = true;

			for (BuildbotPlatformJob tbJob : tinderBoxThreadList) {
				if (!tbJob.isReady()) {
					done = false;
				}
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
		//control.notifyGerritJobFinished(this);
		control.finishGerritJob(this);
	}

	public void createTBResultList() {
		tbResultList = new ArrayList<TbJobResult>();
		// GET AND REGISTER RESULTS
		for (BuildbotPlatformJob tbJob : tinderBoxThreadList) {
			tbResultList.add(tbJob.getResult());
		}
	}

	public void poulateTBPlatformQueueMap(
			Map<Platform, TBBlockingQueue> tbQueueMap) {
		for (int i = 0; i < Platform.values().length; i++) {
			Platform platform = Platform.values()[i];
			BuildbotPlatformJob tbJob = new BuildbotPlatformJob(this, platform);
			tinderBoxThreadList.add(tbJob);
			tbQueueMap.get(platform).add(tbJob);
			tbJob.start();
		}
	}

	public int getId() {
		return id;
	}

	BuildbotPlatformJob getTbJob(String ticket) {
		for (BuildbotPlatformJob job : tinderBoxThreadList) {
			if (!job.isStarted()) {
				continue;
			}

			if (job.getTicketString() != null
					&& job.getTicketString().equals(ticket)) {
				return job;
			}
		}
		return null;
	}

	public TbJobResult setResultPossible(String ticket, String log, boolean status) {
		BuildbotPlatformJob job = getTbJob(ticket);
		if (job != null) {
			if (job.getResult() != null) {
                // result already set: ignore
                return null;
            }
            TbJobResult jobResult = job.createResult(log, status);
			return jobResult;
		}
		return null;
	}

	public long getStartTime() {
		return startTime;
	}

	public List<BuildbotPlatformJob> getBuildbotList() {
		return tinderBoxThreadList;
	}

	public String getGerritRef() {
		return gerritRef;
	}

	public List<TbJobResult> getTbResultList() {
		return tbResultList;
	}

	public String getGerritRevision() {
		return gerritRevision;
	}
}
