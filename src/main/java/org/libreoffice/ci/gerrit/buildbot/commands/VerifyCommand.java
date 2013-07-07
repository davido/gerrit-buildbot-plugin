/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.review.ApproveOption;
import org.libreoffice.ci.gerrit.buildbot.review.ReviewPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.common.data.LabelType;
import com.google.gerrit.common.data.LabelValue;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.util.cli.CmdLineParser;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
@CommandMetaData(name="verify", descr="Manually reset the build outcome")
public final class VerifyCommand extends BuildbotSshCommand {
	static final Logger log = LoggerFactory.getLogger(VerifyCommand.class);

    @Inject
    private IdentifiedUser user;

    @Inject
    private ProjectControl.Factory projectControlFactory;

    @Inject
    private AllProjectsName allProjects;

    @Inject
    private ReviewPublisher publisher;

    @Inject
    protected ReviewDb db;

	private final Set<PatchSet.Id> patchSetIds = new HashSet<PatchSet.Id>();

	private List<ApproveOption> optionList;
	
	@Override
	protected final CmdLineParser newCmdLineParser(Object options) {
		final CmdLineParser parser = super.newCmdLineParser(options);
		for (ApproveOption c : optionList) {
			parser.addOption(c, c);
		}
		return parser;
	}
	
	@Option(name = "--project", aliases = { "-p" }, required = true, metaVar = "PROJECT", usage = "name of the project for patch set to be verified")
	private ProjectControl projectControl;

	@Argument(index = 0, required = true, multiValued = false, metaVar = "{COMMIT | CHANGE,PATCHSET}", usage = "commit or patch set to verify")
	void addPatchSetId(final String token) {
		try {
			patchSetIds.addAll(parsePatchSetId(token));
		} catch (UnloggedFailure e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (OrmException e) {
			throw new IllegalArgumentException("database error", e);
		}
	}

	@Override
	public void doRun() throws UnloggedFailure, OrmException {
	    synchronized (control) {
    		log.debug("verify");
    		final String p = projectControl.getProject().getName();
    		if (!config.isProjectSupported(p)) {
    			String tmp = String.format(
    					"error: project %s is not supported", p);
    			log.warn(tmp);
    			stderr.print(tmp + "\n");
    			return;
    		}
    		if (patchSetIds.size() > 1) {
    			String tmp = "error: only one commit|patch set can be provided";
    			log.warn(tmp);
    			stderr.print(tmp + "\n");
    			return;
    		}
    		for (PatchSet.Id id : patchSetIds) {
    			doVerify(id);
    		}
	    }
	}

	private void doVerify(PatchSet.Id id) throws OrmException {
		PatchSet patchSet = db.patchSets().get(id);
		// check if build job is pending for this patch set
		GerritJob job = control.findJobByRevision(projectControl.getProject().getName(),
				patchSet.getRevision()
				.toString());
		if (job != null) {
			String tmp = String.format(
					"error: build job still pending from %s",
					job.getStartTime());
			log.warn(tmp);
			stderr.print(tmp + "\n");
			return;
		}
		overrideVerifyStatus(patchSet);
	}

	private void overrideVerifyStatus(PatchSet ps) {
		Short v = 0;
		for (ApproveOption ao : optionList) {
			v = ao.value();
		}
		final String changeComment = String
				.format("Verification status of the Buildbot is manually set to %d by %s",
						v, user.getUserName());
		try {
		  publisher.approveOne(ps.getId(), changeComment, optionList);
		} catch (Exception e) {
        	String tmp = String.format("fatal: internal server error while approving %s\n", ps.getId());
        	writeError(tmp);
        	log.error(tmp, e);
			die(e);
        }
	}

	private Set<PatchSet.Id> parsePatchSetId(final String patchIdentity)
			throws UnloggedFailure, OrmException {
		// By commit?
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
	
	@Override
	protected void parseCommandLine() throws UnloggedFailure {
		optionList = new ArrayList<ApproveOption>();
		
	    ProjectControl allProjectsControl;
	    try {
	      allProjectsControl = projectControlFactory.controlFor(allProjects);
	    } catch (NoSuchProjectException e) {
	      throw new UnloggedFailure("missing " + allProjects.get());
	    }

        for (LabelType type : allProjectsControl.getLabelTypes()
                .getLabelTypes()) {

            if (!type.getName().equals("Verified")) {
                continue;
            }

            String usage = "";
            usage = "score for " + type.getName() + "\n";

            for (LabelValue v : type.getValues()) {
                usage += v.format() + "\n";
            }

            final String name = "--" + type.getName().toLowerCase();
            optionList.add(new ApproveOption(name, usage, type));
        }

		super.parseCommandLine();
	}

}
