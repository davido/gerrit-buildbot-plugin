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

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;

import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public interface LogicControl {
	boolean isProjectSupported(String project);
	void startGerritJob(PatchSetCreatedEvent event);
	void startGerritJob(CommentAddedEvent event);
	void startGerritJob(Change change, PatchSet patchSet);
	List<GerritJob> getGerritJobs();
	GerritJob findJobByRevision(String revision);
	TbJobDescriptor launchTbJob(Platform platform, String box);
	TbJobResult setResultPossible(String ticket, TaskStatus status, String log);
}
