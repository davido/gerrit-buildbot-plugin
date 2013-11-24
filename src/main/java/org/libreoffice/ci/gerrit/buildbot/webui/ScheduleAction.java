/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.webui;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.libreoffice.ci.gerrit.buildbot.commands.ScheduleCommand;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.webui.UiAction;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gwtorm.server.AtomicUpdate;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ScheduleAction implements UiAction<RevisionResource>,
    RestModifyView<RevisionResource, ScheduleAction.Input> {
  private static final Logger log = LoggerFactory
      .getLogger(ScheduleCommand.class);

  private final Provider<CurrentUser> cu;
  private final BuildbotLogicControl control;
  private final BuildbotConfig config;
  private final Provider<ReviewDb> dbProvider;

  @Inject
  public ScheduleAction(Provider<CurrentUser> cu,
      BuildbotLogicControl control,
      BuildbotConfig config,
      Provider<ReviewDb> dbProvider) {
    this.cu = cu;
    this.control = control;
    this.config = config;
    this.dbProvider = dbProvider;
  }

  @Override
  public UiAction.Description getDescription(RevisionResource rcrs) {
    synchronized (control) {
      // check if the project is supported
      String p =
          rcrs.getControl().getProjectControl().getProject().getName();
      if (!config.isProjectSupported(p)) {
        log.debug(String.format("getDescription: empty, project: %s is not supported", p));
        return new Description().setVisible(false);
      }
      GerritJob job = findBuild4Revision(rcrs);
      return new Description()
          .setVisible(isVisible(rcrs))
          .setEnabled(job == null)
          .setLabel("Schedule...")
          .setTitle(job == null
              ? "Schedule a build"
              : String.format("Build job was already scheduled at %s",
                  time(job.getStartTime())));
    }
  }

  private boolean isVisible(RevisionResource rcrs) {
    log.debug("-> isVisible()");
    PatchSet.Id current = rcrs.getChange().currentPatchSetId();
    // only current revision
    if (!rcrs.getPatchSet().getId().equals(current)) {
      return false;
    }
    CurrentUser user = cu.get();
    // check if the user is logged in
    if (!(user instanceof IdentifiedUser)) {
      log.debug("negative: user not logged in");
      return false;
    }
    // check if the project is supported
    String p =
        rcrs.getControl().getProjectControl().getProject().getName();
    if (!config.isProjectSupported(p)) {
      log.debug(String.format("negative: project %s is not supported", p));
      return false;
    }
    // check if the user has the ACL
    if (!user.getEffectiveGroups().contains(
        config.findProject(p).getBuildbotUserGroupId())
        && !user.getEffectiveGroups().contains(
            config.findProject(p).getBuildbotAdminGroupId())) {
      log.debug("negative: user is not allowed to schedule a job");
      return false;
    }
    // check if the patch set is NEW
    if (rcrs.getChange().getStatus() != null) {
      if (Change.Status.NEW != rcrs.getChange().getStatus()) {
        log.debug(String.format("negative: status of patch set %s is not NEW",
            rcrs.getChange().getId().toString()));
        return false;
      }
    }
    final BuildbotProject buildbot = config.findProject(p);
    // check if the branch is enabled
    if (!buildbot.getBranches().isEmpty()) {
      if (!buildbot.getBranches().contains(
          rcrs.getChange().getDest().getShortName())) {
        log.debug(String.format("negative: branch not match %s", rcrs
            .getChange().getDest().getShortName()));
        return false;
      }
    }
    // TODO: check if the ps was already successful built
    log.debug("<- true");
    return true;
  }

  @Override
  public Object apply(RevisionResource rcrs, Input in)
      throws OrmException, ResourceConflictException {
    synchronized (control) {
      final GerritJob job = findBuild4Revision(rcrs);
      String msg;
      if (job != null) {
        msg =
            String.format("Build job was already scheduled: %s, at: %s",
                job.getId(), time(job.getStartTime()));
      } else {
        Change change = rcrs.getChange();
        log.debug("dispatch event branch: {}, ref: {}", change.getDest()
            .getShortName(), rcrs.getPatchSet().getRefName());
        control.startGerritJob(rcrs.getControl().getProject().getName(),
            change, rcrs.getPatchSet());
        msg =
            String.format("Build job scheduled: %s", rcrs.getPatchSet().getId()
                .toString());
        ReviewDb db = dbProvider.get();
        db.changes().beginTransaction(change.getId());
        try {
          change = db.changes().atomicUpdate(
            change.getId(),
            new AtomicUpdate<Change>() {
              @Override
              public Change update(Change change) {
                ChangeUtil.updated(change);
                return change;
              }
            });
          db.commit();
        } finally {
          db.rollback();
        }
      }
      return msg;
    }
  }

  private GerritJob findBuild4Revision(RevisionResource rcrs) {
    return control.findJobByPatchSet(rcrs.getControl().getProject().getName(),
        rcrs.getPatchSet());
  }

  private static String time(final long now) {
    final Date when = new Date(now);
    return new SimpleDateFormat("MMM-dd HH:mm").format(when);
  }

  static class Input {
  }
}
