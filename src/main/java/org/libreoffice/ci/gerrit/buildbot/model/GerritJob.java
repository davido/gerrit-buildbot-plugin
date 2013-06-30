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
import java.util.Set;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.logic.impl.ProjectControlImpl;

import com.google.common.collect.Sets;

public class GerritJob implements Runnable {
    String gerritProject;
    String gerritChange;
    String gerritBranch;
    String gerritRef;
    String gerritRevision;
    Thread thread;
    String id;
    long startTime;

    final List<BuildbotPlatformJob> tinderBoxThreadList = Collections
            .synchronizedList(new ArrayList<BuildbotPlatformJob>());
    List<TbJobResult> tbResultList;
    ProjectControlImpl control;
    private boolean stale;

    public GerritJob(ProjectControlImpl control, String project, String change,
            String gerritBranch, String gerritRef, String gerritRevision) {
        this.control = control;
        this.gerritProject = project;
        this.gerritChange = change;
        this.gerritBranch = gerritBranch;
        this.gerritRef = gerritRef;
        this.gerritRevision = gerritRevision;
        this.id = abbreviate(gerritRevision);
        this.startTime = System.currentTimeMillis();
    }

    /** Obtain a shorter version of this key string, using a leading prefix. */
    public String abbreviate(String s) {
        return s.substring(0, Math.min(s.length(), 9));
    }

    public void start() {
        thread = new Thread(this, "name");
        thread.start();
    }

    public String getGerritChange() {
        return gerritChange;
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

            // important to synchronise here
            // because setResultPossible() might
            // replace a cancelled task
            synchronized (tinderBoxThreadList) {
                for (BuildbotPlatformJob tbJob : tinderBoxThreadList) {
                    if (!tbJob.isReady()) {
                        done = false;
                    }
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }
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
            Map<Os, TBBlockingQueue> tbQueueMap) {
        for (int i = 0; i < Os.values().length; i++) {
            Os platform = Os.values()[i];
            initPlatformJob(tbQueueMap, platform);
        }
    }

    private void initPlatformJob(Map<Os, TBBlockingQueue> tbQueueMap,
            Os platform) {
        synchronized (tinderBoxThreadList) {
            BuildbotPlatformJob tbJob = new BuildbotPlatformJob(this, platform);
            tinderBoxThreadList.add(tbJob);
            tbQueueMap.get(platform).add(tbJob);
            tbJob.start();
        }
    }

    public String getId() {
        return id;
    }

    public BuildbotPlatformJob getTbJob(String ticket) {
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

    public TbJobResult setResultPossible(String ticket, String boxId,
            String log, TaskStatus status) {
        BuildbotPlatformJob task = getTbJob(ticket);
        if (task == null || task.getResult() != null) {
            return null;
        }

        if (!task.getTinderboxId().equals(boxId)) {
            // tinderbox doesn't match, ignore
            return null;
        }

        synchronized (tinderBoxThreadList) {
            Set<BuildbotPlatformJob> discardedTasks = Sets.newHashSet();
            // Before we report a status back, check different
            // strategies/optimisations
            // 1. if status is failed, then discard all pending tasks.
            if (status.isFailed()) {
                for (BuildbotPlatformJob task2 : tinderBoxThreadList) {
                    if (task2.getTicketString() != null
                            && task2.getTicketString().equals(ticket)) {
                        // skip the same task
                        continue;
                    }
                    // discard pending tasks
                    if (task2.isDiscardable()) {
                        discardedTasks.add(task2);
                        task2.discard();
                    }
                }
            }
            TbJobResult jobResult = task.createResult(log, status, boxId, discardedTasks);
            // 2. if status is canceled, the reschedule a new task for the same
            // platform
            // reuse the same id and drop the old task from the list (replace
            // it)
            // Important to synchronie the block, so that the job is not ready.
            if (status.isCancelled()) {
                tinderBoxThreadList.remove(task);
                initPlatformJob(control.getTbQueueMap(), task.platform);
            }
            return jobResult;
        }
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

    /**
     * If a new patch for a change is 'submitted' while the verification tinbuild are pending then
     * If no tasks for the job are running, then the whole job is dropped
     * we let already running tindebox finish, and still report the result
     * in the 'review' comment as usual, but leave the verify flags untouched
     * any platform that is not started yet is 'discarded' for that patch.
     * IOW the tasks that are not started yet are de-queued by 'Submit'.
     **/
    public void handleStale(Map<Os, TBBlockingQueue> tbQueueMap) {
        this.setStale(true);
        synchronized (tinderBoxThreadList) {
            for (BuildbotPlatformJob task : tinderBoxThreadList) {
                if (task.isDiscardable()) {
                    task.discard();
                    tbQueueMap.get(task.getPlatform()).remove(task);
                }
            }
        }
    }

    public boolean isStale() {
        return stale;
    }

    private void setStale(boolean stale) {
        this.stale = stale;
    }
}
