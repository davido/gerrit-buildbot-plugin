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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.logic.ProjectControl;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.Os;
import org.libreoffice.ci.gerrit.buildbot.model.TBBlockingQueue;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public class ProjectControlImpl implements ProjectControl {

    static final Logger log = LoggerFactory.getLogger(ProjectControl.class);
    private final Map<Os, TBBlockingQueue> tbQueueMap = new ConcurrentHashMap<Os, TBBlockingQueue>();
    private final List<GerritJob> gerritJobList = Collections
            .synchronizedList(new ArrayList<GerritJob>());

    public ProjectControlImpl() {
    }

    @Override
    public void start() {
        log.debug("started");
        for (int i = 0; i < Os.values().length; i++) {
            tbQueueMap.put(Os.values()[i],
                    new TBBlockingQueue(Os.values()[i]));
        }
    }

    @Override
    public void stop() {
        log.debug("stopped");
        synchronized (gerritJobList) {
            for (GerritJob job : gerritJobList) {
                for (BuildbotPlatformJob task : job.getBuildbotList()) {
                    task.setAbort(true);
                }
            }
            gerritJobList.clear();
            tbQueueMap.clear();
        }
    }

    private TBBlockingQueue getQueue(Os p) {
        return tbQueueMap.get(p);
    }

    public void finishGerritJob(GerritJob job) {
        synchronized (gerritJobList) {
            gerritJobList.remove(job);
        }
    }

    public void startGerritJob(String project, String change, String branch, String ref,
            String revision) {
        synchronized (gerritJobList) {
            GerritJob job = new GerritJob(this, project, change, branch, ref, revision);
            startJob(job);
        }
    }

    public void startGerritJob(PatchSetCreatedEvent event) {
        synchronized (gerritJobList) {
            if (log.isDebugEnabled()) {
                log.debug("startGerritJob: {} {} {} {} {}", new String[] {
                        event.change.project,
                        event.change.id,
                        event.change.branch,
                        event.patchSet.ref,
                        event.patchSet.revision });
            }
            startGerritJob(event.change.project, event.change.id,
                    event.change.branch,
                    event.patchSet.ref,
                    event.patchSet.revision);
        }
    }

    public void startGerritJob(CommentAddedEvent event) {
        synchronized (gerritJobList) {
            if (log.isDebugEnabled()) {
                log.debug("startGerritJob: {} {} {} {} {}", new String[] {
                        event.change.project,
                        event.change.id,
                        event.change.branch,
                        event.patchSet.ref,
                        event.patchSet.revision });
            }
            startGerritJob(event.change.project, event.change.id, event.change.branch,
                    event.patchSet.ref, event.patchSet.revision);
        }
    }

    public void startGerritJob(Change change, PatchSet patchSet) {
        synchronized (gerritJobList) {
            if (log.isDebugEnabled()) {
                log.debug("CommentAddedEvent: {} {} {} {} {}", new String[] {
                        change.getProject().get(),
                        change.getKey().abbreviate(),
                        change.getDest().getShortName(),
                        patchSet.getRefName(),
                        patchSet.getRevision().get() });
            }
            startGerritJob(change.getProject().get(), change.getKey().toString(), change.getDest()
                    .getShortName(), patchSet.getRefName(), patchSet
                    .getRevision().get());
        }
    }

    private void startJob(GerritJob job) {
        synchronized (gerritJobList) {
            if (log.isDebugEnabled()) {
                log.debug("start job {}", job.getId());
            }
            job.poulateTBPlatformQueueMap(tbQueueMap);
            gerritJobList.add(job);
            job.start();
        }
    }

    @Override
    public void handleStaleJob(GerritJob job) {
        job.handleStale(tbQueueMap);
    }

    public TbJobResult setResultPossible(String ticket, String boxId,
            TaskStatus status, String logurl) {
        synchronized (gerritJobList) {
            TbJobResult jobResult = null;
            if (log.isDebugEnabled()) {
                log.debug("set result {}, {}, {}", new String[] { ticket,
                        boxId, status.name() });
            }
            for (GerritJob job : gerritJobList) {
                jobResult = job
                        .setResultPossible(ticket, boxId, logurl, status);
                if (jobResult != null) {
                    Set<BuildbotPlatformJob> discardedTasks = jobResult
                            .getDiscardedTasks();
                    for (BuildbotPlatformJob task : discardedTasks) {
                        log.debug("remove discarded task: {} for {}", task
                                .getParent().getId(), task.getPlatform().name());
                        getQueue(task.getPlatform()).remove(task);
                    }
                    if (job.allJobsReady()) {
                        job.createTBResultList();
                    }
                    break;
                }
            }
            if (log.isDebugEnabled()) {
                // dump queues
                for (Os p : Os.values()) {
                    TBBlockingQueue platformQueue = getQueue(p);
                    if (!platformQueue.isEmpty()) {
                        Set<String> emptyBranchSet = Collections.emptySet();
                        BuildbotPlatformJob tbJob = platformQueue
                                .peek(emptyBranchSet);
                        log.debug("pending task: {}", tbJob.getPlatform()
                                .name());
                    }
                }
            }
            return jobResult;
        }
    }

    public TbJobDescriptor launchTbJob(Os platform,
            Set<String> branchSet, String box, boolean test) {
        synchronized (gerritJobList) {
            if (log.isDebugEnabled()) {
                log.debug("{} task {}, {}", new String[] {
                        test ? "peek" : "poll", platform.name(), box });
            }
            TBBlockingQueue platformQueue = getQueue(platform);
            BuildbotPlatformJob tbJob = test ? platformQueue.peek(branchSet)
                    : platformQueue.poll(branchSet);
            if (tbJob == null) {
                return null;
            }
            if (test) {
                tbJob.testBuildOnly(platform);
            } else {
                tbJob.createAndSetTicket(platform, box);
            }
            return new TbJobDescriptor(tbJob);
        }
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

    public List<GerritJob> getGerritJobs() {
        synchronized (gerritJobList) {
            return gerritJobList;
        }
    }

    @Override
    public GerritJob findJobByRevision(String revision) {
        synchronized (gerritJobList) {
            for (GerritJob job : gerritJobList) {
                if (job.getGerritRevision().equals(revision)) {
                    return job;
                }
            }
        }
        return null;
    }

    @Override
    public GerritJob findJobById(String id) {
        synchronized (gerritJobList) {
            for (GerritJob job : gerritJobList) {
                if (job.getId().equals(id)) {
                    return job;
                }
            }
        }
        return null;
    }

    @Override
    public GerritJob findJobByChange(String change) {
        synchronized (gerritJobList) {
            for (GerritJob job : gerritJobList) {
                if (job.getGerritChange().equals(change)) {
                    return job;
                }
            }
        }
        return null;
    }

    @Override
    public GerritJob findJobByTicket(String ticket) {
        synchronized (gerritJobList) {
            for (GerritJob job : gerritJobList) {
                BuildbotPlatformJob task = job.getTbJob(ticket);
                if (task != null) {
                    return job;
                }
            }
        }
        return null;
    }

    public Map<Os, TBBlockingQueue> getTbQueueMap() {
        return tbQueueMap;
    }
}
