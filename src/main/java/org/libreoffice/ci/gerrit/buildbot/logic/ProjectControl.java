/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.Os;
import org.libreoffice.ci.gerrit.buildbot.model.TBBlockingQueue;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;

import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public interface ProjectControl {
    void startGerritJob(String project, String change, String branch, String ref, String revision);
	void startGerritJob(PatchSetCreatedEvent event);
	void startGerritJob(CommentAddedEvent event);
	void startGerritJob(Change change, PatchSet patchSet);
	List<GerritJob> getGerritJobs();
	GerritJob findJobById(String id);
	GerritJob findJobByRevision(String revision);
	GerritJob findJobByTicket(String ticket);
	GerritJob findJobByChange(String change);
	TbJobDescriptor launchTbJob(Os platform, Set<String> branch, String box, boolean test);
	TbJobResult setResultPossible(String ticket, String boxId, TaskStatus status, String logurl);
	void stop();
	void start();
	Map<Os, TBBlockingQueue> getTbQueueMap();
    void handleStaleJob(GerritJob job);
}
