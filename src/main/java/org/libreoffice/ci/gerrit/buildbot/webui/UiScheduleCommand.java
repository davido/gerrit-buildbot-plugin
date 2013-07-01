/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
    private static final Logger log = LoggerFactory
            .getLogger(ScheduleCommand.class);

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
        return "Are you really sure, you want to schedule a build?\n "
                + "Have you read RTFM?\n "
                + "Have you checked the queue status?\n "
                + "If not sure, please ask on #libreoffice-dev first!";
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
                    "Build job was already scheduled at %s",
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
        log.debug("-> isVisible()");
        synchronized (control) {
            final CurrentUser user = cu.get();
            // check if the user is logged in
            if (!(user instanceof IdentifiedUser)) {
                log.debug("negative: user not logged in");
                return false;
            }
            // check if the project is supported
            final String p = rcrs.getControl().getProjectControl().getProject()
                    .getName();
            if (!config.isProjectSupported(p)) {
                log.debug(String.format(
                        "negative: project %s is not supported", p));
                return false;
            }
            // check if the user has the ACL
            if (!user.getEffectiveGroups().contains(
                    config.findProject(p).getBuildbotUserGroupId())
                    &&
                !user.getEffectiveGroups().contains(
                    config.findProject(p).getBuildbotAdminGroupId())) {
                log.debug("negative: user is not allowed to schedule a job");
                return false;
            }
            // check if the patch set is NEW
            if (rcrs.getChange().getStatus() != null) {
                if (Change.Status.NEW != rcrs.getChange().getStatus()) {
                    log.debug(String.format(
                            "negative: status of patch set %s is not NEW", rcrs
                                    .getChange().getId().toString()));
                    return false;
                }
            }
            final BuildbotProject buildbot = config.findProject(p);
            // check if the branch is enabled
            if (!buildbot.getBranches().isEmpty()) {
                if (!buildbot.getBranches().contains(
                        rcrs.getChange().getDest().getShortName())) {
                    log.debug(String.format("negative: branch not match %s",
                            rcrs.getChange().getDest().getShortName()));
                    return false;
                }
            }
        }
        log.debug("<- true");
        return true;
    }

    @Override
    public Result apply(RevisionResource rcrs, Input in) {
        synchronized (control) {
            final GerritJob job = findBuild4Revision(rcrs);
            String msg;
            if (job != null) {
                msg = String.format(
                        "Build job was already scheduled: %s, at: %s",
                        job.getId(), time(job.getStartTime()));
            } else {
                final Change change = rcrs.getChange();
                log.debug("dispatch event branch: {}, ref: {}", change
                        .getDest().getShortName(), rcrs.getPatchSet()
                        .getRefName());
                control.startGerritJob(
                        rcrs.getControl().getProject().getName(), change,
                        rcrs.getPatchSet());
                msg = String.format("Build job successfully scheduled: %s",
                        rcrs.getPatchSet().getId().toString());
            }
            Result result = new Result();
            result.message = msg;
            result.action = Result.Action.RELOAD;
            return result;
        }
    }

    private GerritJob findBuild4Revision(RevisionResource rcrs) {
        return control.findJobByPatchSet(rcrs.getControl().getProject()
                .getName(), rcrs.getPatchSet());
    }

    private static String time(final long now) {
        final Date when = new Date(now);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }

    static class Input {
    }
}
