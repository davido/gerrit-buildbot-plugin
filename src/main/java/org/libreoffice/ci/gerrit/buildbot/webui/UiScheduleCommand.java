package org.libreoffice.ci.gerrit.buildbot.webui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

import org.libreoffice.ci.gerrit.buildbot.commands.ScheduleCommand;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiCommand;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.change.RevisionResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class UiScheduleCommand implements UiCommand<RevisionResource>,
		RestModifyView<RevisionResource, UiScheduleCommand.Input> {
	static final Logger log = LoggerFactory.getLogger(ScheduleCommand.class);

	private final Provider<CurrentUser> cu;
	private final BuildbotLogicControl control;
	private final BuildbotConfig config;

	@Inject
	public UiScheduleCommand(Provider<CurrentUser> cu,
			BuildbotLogicControl control, BuildbotConfig config) {
		this.cu = cu;
		this.control = control;
		this.config = config;
	}

	@Override
	public String getConfirmationMessage(RevisionResource rcrs) {
		return null;
	}

	@Override
	public String getLabel(RevisionResource rcrs) {
		return "Schedule";
	}

	@Override
	public EnumSet<Place> getPlaces() {
		return EnumSet.of(UiCommand.Place.CURRENT_PATCHSET_ACTION_PANEL);
	}

	@Override
	public String getTitle(RevisionResource rcrs) {
		synchronized (control) {
			final GerritJob job = findBuild4Revision(rcrs);
			return job == null ? "Schedule a build" : String.format(
					"Build job already scheduled at %s",
					time(job.getStartTime()));
		}
	}

	@Override
	public boolean isEnabled(RevisionResource rcrs) {
		synchronized (control) {
			return findBuild4Revision(rcrs) == null;
		}
	}

	@Override
	public boolean isVisible(RevisionResource rcrs) {
		synchronized (control) {
			final CurrentUser user = cu.get();
			if (!(user instanceof IdentifiedUser)) {
				// check if the user is logged in
				log.debug("not visible: user not logged in");
				return false;
			}
			final String p = rcrs.getControl().getProjectControl().getProject()
					.getName();
			// check if the project is supported
			if (!config.isProjectSupported(p)) {
				log.debug(String.format(
						"not visible: project %s is not supported", p));
			}
			if (!rcrs.getControl().getRefControl().canWrite()) {
				// check if the user has the ACL
				log.debug("not visible: user can not write");
				return false;
			}
			if (rcrs.getChange().getStatus() != null) {
				// check if the patch set is NEW
				if (Change.Status.NEW != rcrs.getChange().getStatus()) {
					log.debug(String.format(
							"not visible: status of patch set %s is not NEW",
							rcrs.getChange().getId().toString()));
					return false;
				}
			}
			BuildbotProject buildbot = config.findProject(p);
			// check if branch configured
			if (!buildbot.getBranches().isEmpty()) {
				if (!buildbot.getBranches().contains(
						rcrs.getChange().getDest().getShortName())) {
					log.debug(String.format("not visible: branch not match %s",
							rcrs.getChange().getDest().getShortName()));
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Result apply(RevisionResource rcrs, Input in) {
		synchronized (control) {
			GerritJob job = findBuild4Revision(rcrs);
			String msg;
			if (job != null) {
				msg = String.format(
						"build is already tunning for patch set: %s",
						job.getId());
			} else {
				Change change = rcrs.getChange();
				log.debug("dispatch event branch: {}, ref: {}", change
						.getDest().getShortName(), rcrs.getPatchSet()
						.getRefName());
				control.startGerritJob(
						rcrs.getControl().getProject().getName(), change,
						rcrs.getPatchSet());
				job = findBuild4Revision(rcrs);
				msg = String.format("build scheduled for patch set: %s",
						job.getId());
			}
			Result result = new Result();
			result.message = msg;
			result.action = Result.Action.NONE;
			return result;
		}
	}

	private GerritJob findBuild4Revision(RevisionResource rcrs) {
		return control.findJobByRevision(rcrs.getControl().getProject()
				.getName(), rcrs.getPatchSet().getRevision().toString());
	}

	private static String time(final long now) {
		final Date when = new Date(now);
		return new SimpleDateFormat("MMM-dd HH:mm").format(when);
	}

	static class Input {
	}
}
