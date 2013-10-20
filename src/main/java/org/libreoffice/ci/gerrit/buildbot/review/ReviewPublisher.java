/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.review;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.change.PostReview;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.project.ChangeControl;
import com.google.gerrit.server.project.NoSuchChangeException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ReviewPublisher {
  static final Logger log = LoggerFactory.getLogger(ReviewPublisher.class);
  private final BuildbotLogicControl control;
  private final BuildbotConfig config;
  private final Provider<ReviewDb> db;
  private final Provider<PostReview> reviewProvider;
  private final ChangeControl.Factory changeControlFactory;

  @Inject
  public ReviewPublisher(BuildbotLogicControl control, BuildbotConfig config,
      Provider<ReviewDb> db, Provider<PostReview> reviewProvider,
      ChangeControl.Factory changeControlFactory) {
    this.control = control;
    this.config = config;
    this.db = db;
    this.reviewProvider = reviewProvider;
    this.changeControlFactory = changeControlFactory;
  }

  public void postResultToReview(TbJobResult result) throws OrmException {
    Preconditions.checkNotNull(result);
    notifyGerritBuildbotPlatformJobFinished(result,
        retrievePatchSet(result.getTbPlatformJob().getParent()));
  }

  public void approveOne(GerritJob job, final String changeComment,
      final List<ApproveOption> optionList) throws NoSuchChangeException,
      OrmException, BadRequestException, AuthException,
      UnprocessableEntityException, IOException{
    approveOne(retrievePatchSet(job).getId(), changeComment, optionList);
  }
  
  public void approveOne(final PatchSet.Id patchSetId,
      final String changeComment, final List<ApproveOption> optionList)
      throws NoSuchChangeException, OrmException, BadRequestException,
      AuthException, UnprocessableEntityException, IOException {
    PostReview.Input review = createReview(changeComment);
    for (ApproveOption ao : optionList) {
      Short v = ao.value();
      if (v != null) {
        review.labels.put(ao.getLabelName(), v);
      }
    }
    applyReview(patchSetId, review);
  }

  private PatchSet retrievePatchSet(GerritJob job)
      throws OrmException {
    final List<PatchSet> matches =
        db.get()
            .patchSets()
            .byRevision(
                new RevId(job.getGerritRevision())).toList();
    if (matches.size() != 1) {
      String tmp =
          String.format("Can not match patch set for revision %s", job.getGerritRevision());
      log.error(tmp);
      throw new OrmException("Can not find patch set: " + job.getGerritRevision());
    }
    return matches.get(0);
  }

  private void notifyGerritBuildbotPlatformJobFinished(TbJobResult tbJobResult,
      final PatchSet ps) {
    short status = 0;
    String msg =
        String.format("%s %s (%s)\n\nBuild on %s at %s: %s", tbJobResult
            .getPlatform().name(), tbJobResult.getStatus().name(), tbJobResult
            .getDecoratedId(), tbJobResult.getTinderboxId(), time(tbJobResult
            .getEndTime()), Strings.nullToEmpty(tbJobResult.getLog()));
    try {
      approveOne(ps.getId(), msg, "Code-Review", status);
    } catch (Exception e) {
      log.error(
          String.format("fatal: internal server error while approving %s\n",
              ps.getId()), e);
    }
  }

  public void approveOne(GerritJob job, final String changeComment,
      final String labelName, final short value) throws NoSuchChangeException,
      OrmException, BadRequestException, AuthException,
      UnprocessableEntityException, IOException{
    approveOne(retrievePatchSet(job).getId(), changeComment, labelName, value);
  }

  private void approveOne(final PatchSet.Id patchSetId,
      final String changeComment, final String labelName, final short value)
      throws NoSuchChangeException, OrmException, BadRequestException,
      AuthException, UnprocessableEntityException, IOException {
    PostReview.Input review = createReview(changeComment);
    review.labels.put(labelName, value);
    applyReview(patchSetId, review);
  }

  private void applyReview(final PatchSet.Id patchSetId, PostReview.Input review)
      throws NoSuchChangeException, OrmException, BadRequestException, AuthException,
      UnprocessableEntityException, IOException {
    ChangeControl ctl =
        changeControlFactory.controlFor(patchSetId.getParentKey());
    if (!review.labels.isEmpty()) {
      RevisionResource rsrc =
          new RevisionResource(new ChangeResource(ctl), db.get().patchSets()
              .get(patchSetId));
      forgeReviewerIdentity(rsrc);
      reviewProvider.get().apply(rsrc, review);
    }
  }

  private PostReview.Input createReview(final String changeComment) {
    PostReview.Input review = new PostReview.Input();
    review.message = Strings.emptyToNull(changeComment);
    review.labels = Maps.newTreeMap();
    review.drafts = PostReview.DraftHandling.PUBLISH;
    review.strictLabels = false;
    return review;
  }

  private void forgeReviewerIdentity(RevisionResource rsrc) throws OrmException {
    if (config.isForgeReviewerIdentity()) {
      try {
        Field field = ProjectControl.class.getDeclaredField("user");
        field.setAccessible(true);
        field.set(rsrc.getControl().getProjectControl(), control.getBuildbot());
      } catch (Exception e) {
        throw new OrmException("Can not forge revviewer identity");
      }
    }
  }

  private static String time(final long now) {
    final Date when = new Date(now);
    return new SimpleDateFormat("MMM-dd HH:mm").format(when);
  }
}
