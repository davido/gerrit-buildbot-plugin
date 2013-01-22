/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.ApprovalTypes;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class ScheduleCommand extends SshCommand {
	static final Logger log = LoggerFactory.getLogger(ScheduleCommand.class);

	@Inject
	BuildbotLogicControl control;

	@Inject
	BuildbotConfig config;

	@Inject
	private ApprovalTypes approvalTypes;

	@Inject
	private ReviewDb db;

	private final Set<PatchSet.Id> patchSetIds = new HashSet<PatchSet.Id>();

	@Option(name = "--project", aliases = { "-p" }, required = true, metaVar = "PROJECT", usage = "name of the project for which the job should be scheduled")
	private ProjectControl projectControl;

	@Argument(index = 0, required = true, multiValued = true, metaVar = "{COMMIT | CHANGE,PATCHSET}", usage = "commit(s) or patch set(s) to schedule")
	void addPatchSetId(final String token) {
		try {
			patchSetIds.addAll(parsePatchSetId(token));
		} catch (UnloggedFailure e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (OrmException e) {
			throw new IllegalArgumentException("database error", e);
		}
	}

	protected String getDescription() {
        return "Manually trigger a build for specified patch sets";
    }

	@Override
	public void run() throws UnloggedFailure, Failure, Exception {
	    synchronized (control) {
    		log.debug("schedule");
    		
    		final String p = projectControl.getProject().getName();
    		if (!config.isProjectSupported(p)) {
    			String tmp = String.format(
    					"error: project %s is not supported", p);
    			log.warn(tmp);
    			stderr.print(tmp + "\n");
    			return;
    		}
    		for (PatchSet.Id id : patchSetIds) {
    			doSchedule(id);
    		}
	    }
	}

	private void doSchedule(PatchSet.Id id) throws OrmException {
		PatchSet patchSet = db.patchSets().get(id);

		Change change = db.changes().get(id.getParentKey());

		// this value seems not to be set
		// only check if value is set (new gerrit release)
		if (change.getStatus() != null) {
			// check if the patch set is NEW
			if (Change.Status.NEW != change.getStatus()) {
				String tmp = String.format(
						"error: status of patch set %s is not NEW",
						id.toString());
				log.warn(tmp);
				stderr.print(tmp + "\n");
				return;
			}
		}

		// check if build job is pending for this patch set
		GerritJob job = control.findJobByRevision(projectControl.getProject().getName(),
				patchSet.getRevision()
				.toString());
		if (job != null) {
			String tmp = String.format(
					"error: build job already scheduled at %s",
					job.getStartTime());
			log.warn(tmp);
			stderr.print(tmp + "\n");
			return;
		}

		BuildbotProject p = config.findProject(projectControl.getProject().getName());
		// check branch if configured
		if (!p.getBranches().isEmpty()) {
			if (!p.getBranches().contains(change.getDest().getShortName())) {
				String tmp = String.format("error: branch not match  %s",
						change.getDest().getShortName());
				log.warn(tmp);
				stderr.print(tmp + "\n");
				return;
			}
		}

		 log.debug("dispatch event branch: {}, ref: {}",
                 change.getDest().getShortName(),
                 patchSet.getRefName());
         control.startGerritJob(projectControl.getProject().getName(), change, patchSet);
	}

	private Set<PatchSet.Id> parsePatchSetId(final String patchIdentity)
			throws UnloggedFailure, OrmException {
		// By commit?
		//
		if (patchIdentity.matches("^([0-9a-fA-F]{4," + RevId.LEN + "})$")) {
			final RevId id = new RevId(patchIdentity);
			final ResultSet<PatchSet> patches;
			if (id.isComplete()) {
				patches = db.patchSets().byRevision(id);
			} else {
				patches = db.patchSets().byRevisionRange(id, id.max());
			}

			final Set<PatchSet.Id> matches = new HashSet<PatchSet.Id>();
			for (final PatchSet ps : patches) {
				final Change change = db.changes().get(
						ps.getId().getParentKey());
				if (inProject(change)) {
					matches.add(ps.getId());
				}
			}

			switch (matches.size()) {
			case 1:
				return matches;
			case 0:
				throw error("\"" + patchIdentity + "\" no such patch set");
			default:
				throw error("\"" + patchIdentity
						+ "\" matches multiple patch sets");
			}
		}

		// By older style change,patchset?
		//
		if (patchIdentity.matches("^[1-9][0-9]*,[1-9][0-9]*$")) {
			final PatchSet.Id patchSetId;
			try {
				patchSetId = PatchSet.Id.parse(patchIdentity);
			} catch (IllegalArgumentException e) {
				throw error("\"" + patchIdentity
						+ "\" is not a valid patch set");
			}
			if (db.patchSets().get(patchSetId) == null) {
				throw error("\"" + patchIdentity + "\" no such patch set");
			}
			if (projectControl != null) {
				final Change change = db.changes().get(
						patchSetId.getParentKey());
				if (!inProject(change)) {
					throw error("change " + change.getId() + " not in project "
							+ projectControl.getProject().getName());
				}
			}
			return Collections.singleton(patchSetId);
		}

		throw error("\"" + patchIdentity + "\" is not a valid patch set");
	}

	private boolean inProject(final Change change) {
		if (projectControl == null) {
			// No --project option, so they want every project.
			return true;
		}
		return projectControl.getProject().getNameKey()
				.equals(change.getProject());
	}

	private static UnloggedFailure error(final String msg) {
		return new UnloggedFailure(1, msg);
	}
}
