/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.logic.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.LogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritNotifyListener;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TBBlockingQueue;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;

import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.util.IdGenerator;
import com.google.inject.Inject;

public class LogicControlImpl implements LogicControl {

	@Inject
	IdGenerator generator;
	
	final Map<Platform, TBBlockingQueue> tbQueueMap = new ConcurrentHashMap<Platform, TBBlockingQueue>();
	final List<GerritJob> gerritJobList = Collections
			.synchronizedList(new ArrayList<GerritJob>());
	final List<GerritNotifyListener> gerritNotifiers = Collections
			.synchronizedList(new ArrayList<GerritNotifyListener>());
	private BuildbotConfig config;

	public LogicControlImpl(final BuildbotConfig config) {
		this.config = config;
		for (int i = 0; i < Platform.values().length; i++) {
			tbQueueMap.put(Platform.values()[i],
					new TBBlockingQueue(Platform.values()[i]));
		}
	}

	private TBBlockingQueue getQueue(Platform p) {
		return tbQueueMap.get(p);
	}

	public void finishGerritJob(GerritJob job) {
		synchronized (gerritJobList) {
			gerritJobList.remove(job);
		}
	}

//	// Notifiy all listener about the ready jobs 
//	public void notifyGerritJobFinished(GerritJob job) {
//		for (GerritNotifyListener l : gerritNotifiers) {
//			l.notifyGerritJobFinished(job);
//		}
//	}
//
//	public void addGerritNotifyListener(GerritNotifyListener l) {
//		gerritNotifiers.add(l);
//	}
//
//	public void removeGerritNotifyListener(GerritNotifyListener l) {
//		gerritNotifiers.remove(l);
//	}

	public void startGerritJob(PatchSetCreatedEvent event) {
		synchronized (gerritJobList) {
			GerritJob job = new GerritJob(this, event.change.branch, event.patchSet.ref, event.patchSet.revision, generator.next());
			job.poulateTBPlatformQueueMap(tbQueueMap);
			gerritJobList.add(job);
			job.start();
		}
	}

	public TbJobResult setResultPossible(String ticket, boolean status, String log) {
		synchronized (gerritJobList) {
			for (GerritJob job : gerritJobList) {
				TbJobResult jobResult = job.setResultPossible(ticket, log, status);
				if (jobResult != null) {
					if (job.allJobsReady()) {
						job.createTBResultList();
					}
					return jobResult;
				}
			}
		}
		return null;
	}

	public TbJobDescriptor launchTbJob(Platform platform) {
		TBBlockingQueue platformQueue = getQueue(platform);

		// no waiting jobs in prio queue ?
		BuildbotPlatformJob tbJob = platformQueue.poll();
		if (tbJob == null) {
			return null;
		}
		tbJob.createAndSetTicket(platform);

		TbJobDescriptor desc = new TbJobDescriptor(tbJob);
		return desc;
	}

	public static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public int getGerritJobsCount() {
		synchronized (gerritJobList) {
			return gerritJobList.size();
		}
	}

	@Override
	public boolean isProjectSupported(String project) {
		return config.getProject().equals(project);
	}
	
	public List<GerritJob> getGerritJobs() {
		return gerritJobList;
	}
}
