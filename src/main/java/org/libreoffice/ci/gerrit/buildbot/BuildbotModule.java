/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot;

import static com.google.inject.Scopes.SINGLETON;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.TriggerStrategie;
import org.libreoffice.ci.gerrit.buildbot.logic.LogicControl;
import org.libreoffice.ci.gerrit.buildbot.logic.LogicControlProvider;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gerrit.common.ChangeHooks;
import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.common.data.ApprovalType;
import com.google.gerrit.common.data.ApprovalTypes;
import com.google.gerrit.common.errors.NoSuchGroupException;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.ApprovalCategory;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.events.AccountAttribute;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;

class BuildbotModule extends AbstractModule {
    static final Logger log = LoggerFactory.getLogger(BuildbotModule.class);

    @Inject
    private ChangeHooks hooks;

    // buildbot itself
    IdentifiedUser user;

    @Inject
    SchemaFactory<ReviewDb> schema;

    @Inject
    private ApprovalTypes approvalTypes;

    @Inject
    private IdentifiedUser.GenericFactory identifiedUserFactory;

    @Inject
    private AccountByEmailCache byEmailCache;

    @Inject
    private GroupCache groupCache;

    @Inject
    @PluginData
    private java.io.File pluginDataDir;

    @Inject
    private SitePaths site;

    @Inject
    private Injector creatingInjector;

    private LogicControl control;

    private BuildbotConfig config;

    ApprovalCategory verified;

    ApprovalCategory reviewed;

    AccountGroup.UUID reviewerGroupId;

    private final ChangeListener listener = new ChangeListener() {
        @Override
        public void onChangeEvent(final ChangeEvent event) {
            if (event instanceof PatchSetCreatedEvent) {
                PatchSetCreatedEvent patchSetCreatedEvent = (PatchSetCreatedEvent) event;
                log.debug("patch-set-created project: {}", patchSetCreatedEvent.change.project);
                if (!control.isProjectSupported(patchSetCreatedEvent.change.project)) {
                    log.debug("skip event: buildbot is not activated for project: {} ", patchSetCreatedEvent.change.project);
                    return;
                }
                if (TriggerStrategie.PATCHSET_CREATED != config.getTriggerStrategie()) {
                    log.debug("skip event: non PATCHSET_CREATED trigger strategie for project: {} ", patchSetCreatedEvent.change.project);
                    return;
                }
                log.debug("dispatch event branch: {}, ref: {}",
                        patchSetCreatedEvent.change.branch,
                        patchSetCreatedEvent.patchSet.ref);
                control.startGerritJob(patchSetCreatedEvent);
            } else if (event instanceof CommentAddedEvent) {
                CommentAddedEvent commentAddedEvent = (CommentAddedEvent) event;
                if (TriggerStrategie.POSITIVE_REVIEW != config.getTriggerStrategie()) {
                    log.debug("skip event: non POSITIVE_REVIEW trigger strategie for project: {} ", commentAddedEvent.change.project);
                    return;
                }

                log.debug("investigating commentAddedEvent branch: {}, ref: {}", 
                        commentAddedEvent.change.branch,
                        commentAddedEvent.patchSet.ref);

                if (isEventOriginatorBuildbot(commentAddedEvent)) {
                    log.debug("ignore comment (buildbot is the originator)");
                    return;
                }

                if (checkPositiveReviewApply(commentAddedEvent)) {
                    log.debug("dispatch event branch: {}, ref: {}",
                            commentAddedEvent.change.branch,
                            commentAddedEvent.patchSet.ref);
                    control.startGerritJob(commentAddedEvent);
                }
            }
        }

        private boolean isEventOriginatorBuildbot(
                CommentAddedEvent commentAddedEvent) {
            final AccountAttribute author = commentAddedEvent.author;
            final Set<Account.Id> ids = byEmailCache.get(author.email);
            final Account.Id id = ids.iterator().next();
            IdentifiedUser eventAuthor = identifiedUserFactory.create(id);

            // check if the buildbot itself is the originator of this event
            if (eventAuthor.getAccountId().equals(user.getAccountId())) {
                return true;
            }
            return false;
        }
    };

    class PositiveReviewResultEntry {
        boolean negativeVerify;
        boolean positiveVerify;
        boolean negativeReview;
        boolean positiveReview;
        boolean approvedByMemberOfGroup;
    }

    private boolean checkPositiveReviewApply(CommentAddedEvent commentAddedEvent) {
        // this value seems not to be set
        // only check if value is set (new gerrit release)
        if (commentAddedEvent.change.status != null) {
            // check if the patch set is NEW
            if (Change.Status.NEW != commentAddedEvent.change.status) {
                log.debug("ignore this comment: status of this patch set {} is not NEW", commentAddedEvent.patchSet.revision);
                return false;
            }
        }

        // check if build job is pending for this patch set
        GerritJob job = control.findJobByRevision(commentAddedEvent.patchSet.revision);
        if (job != null) {
            log.debug("ignore this comment: build job was already scheduled at {}", job.getStartTime());
            return false;
        }

        // check branch
        if (config.getBranch() != null) {
        	if (!config.getBranch().equals(commentAddedEvent.change.branch)) {
        		log.debug("ignore this comment: branch not match {}", commentAddedEvent.change.branch);
        		return false;
        	}
        }
        
        List<PatchSetApproval> list = new ArrayList<PatchSetApproval>();
        PatchSet.Id id = null;
        try {
            final ReviewDb db = schema.open();
            try {
                id = PatchSet.Id.fromRef(commentAddedEvent.patchSet.ref);
                ResultSet<PatchSetApproval> approvals = db.patchSetApprovals().byPatchSet(id);
                for (PatchSetApproval patchSetApproval : approvals) {
                    list.add(patchSetApproval);
                }
            } finally {
              db.close();
            }
          } catch (OrmException e) {
            log.error("Cannot load patch set data for " + id.toString(), e);
          }

        List<PositiveReviewResultEntry> res = new ArrayList<PositiveReviewResultEntry>();

        // Check other approvals
        checkApprovals(list, res);

        /**
         * it has no review < 0,
         * it has no verify < 0,
         * it has no verify > 0 and
         * it has at least 1 review > 0 from a user that belongs to the `Reviewer` Group
         */

        boolean positive = false;
        for (PositiveReviewResultEntry positiveReviewResultEntry : res) {
            // it has no review < 0 and it has no verify < 0
            if (positiveReviewResultEntry.negativeReview ||
                positiveReviewResultEntry.negativeVerify) {
                return false;
            }
            // it has no verify > 0
            if (positiveReviewResultEntry.positiveVerify) {
                return false;
            }
            // it has at least 1 review > 0 from a user that belongs to the `Reviewer` Group
            if (positiveReviewResultEntry.positiveReview && positiveReviewResultEntry.approvedByMemberOfGroup) {
                positive = true;
            }
        }
        return positive;
    }

    private void checkApprovals(Collection<PatchSetApproval> approvals,
            List<PositiveReviewResultEntry> res) {
        for (PatchSetApproval attr : approvals) {
            PositiveReviewResultEntry r = new PositiveReviewResultEntry();

            final Account.Id id = attr.getAccountId();
            IdentifiedUser eventAuthor = identifiedUserFactory.create(id);

            log.debug("check approval from user: {}", eventAuthor.getUserName());
            GroupMembership members = eventAuthor.getEffectiveGroups();
            if (members.contains(reviewerGroupId)) {
                r.approvedByMemberOfGroup = true;
                log.debug("user {} is member of Reviewer group", eventAuthor.getUserName());
            }

            if (verified.getId().get().toString().equals(attr.getCategoryId().toString())) {
                short v = Short.valueOf(attr.getValue()).shortValue();
                if (v > 0) {
                    r.positiveVerify = true;
                } else if (v < 0) {
                    r.negativeVerify = true;
                }
            } else if (reviewed.getId().get().toString().equals(attr.getCategoryId().toString())) {
                short v = Short.valueOf(attr.getValue()).shortValue();
                if (v > 0) {
                    r.positiveReview = true;
                } else if (v < 0) {
                    r.negativeReview = true;
                }
            }
            res.add(r);
        }
    }

    @Override
    protected void configure() {
        bind(BuildbotConfig.class).toInstance(config());
        this.control =
                creatingInjector.createChildInjector(new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(BuildbotConfig.class).toInstance(config);
                    bind(LogicControl.class).toProvider(LogicControlProvider.class).in(SINGLETON);
                  }
                }).getInstance(LogicControl.class);
        bind(LogicControl.class).toInstance(control);
        init();
    }

    private BuildbotConfig config() {
        Config cfg = new Config();
        File configFile = null;
        String configContent = null;
        try {
            configFile = new File(site.etc_dir, "buildbot.config");
            configContent = read(configFile);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new IllegalStateException(String.format(
                    "can not find config file: %s",
                    configFile.getAbsolutePath()), ex);
        }

        try {
            cfg.fromText(configContent);
        } catch (ConfigInvalidException ex) {
            log.error(ex.getMessage());
            throw new IllegalStateException(String.format(
                    "can not parse config file: %s",
                    configFile.getAbsolutePath()), ex);
        }
        String email = cfg.getString("user", null, "mail");
        String project = cfg.getString("project", null, "name");
        String branch = cfg.getString("project", null, "branch");
        String strStrategie = cfg.getString("project", null, "trigger");

        Preconditions.checkNotNull(strStrategie, "strategie must not be null");

        String directory = cfg.getString("log", null, "directory");
        config = new BuildbotConfig();
        config.setEmail(email);
        config.setProject(project);
        config.setBranch(branch);

        TriggerStrategie triggerStrategie = TriggerStrategie.valueOf(strStrategie.toUpperCase());
        Preconditions.checkNotNull(triggerStrategie, String.format("unknown strategie %s", strStrategie));

        config.setTriggerStrategie(triggerStrategie);
        if (triggerStrategie == TriggerStrategie.POSITIVE_REVIEW) {
            String reviewerGroupName = cfg.getString("project", null, "reviewerGroupName");
            Preconditions.checkNotNull(reviewerGroupName, "reviewerGroupName must not be null");
            config.setReviewerGroupName(reviewerGroupName);
        }
        config.setLogDir(directory);

        return config;
    }

    private void init() {
        Set<Account.Id> ids = byEmailCache.get(config.getEmail());
        if (CollectionUtils.isEmpty(ids)) {
            throw new IllegalStateException("user not found for email: "
                    + config.getEmail());
        }
        Account.Id id = ids.iterator().next();
        user = identifiedUserFactory.create(id);
        hooks.addChangeListener(listener, user);

        // categories
        for (ApprovalType type : approvalTypes.getApprovalTypes()) {
            final ApprovalCategory category = type.getCategory();
            if ("VRIF".equals(category.getId().get())) {
                verified = category;
            } else if ("CRVW".equals(category.getId().get())) {
                reviewed = category;
            }
        }
        
        if (config.getReviewerGroupName() != null) {        	
        	initReviewerGroup();
        }
    }

	private void initReviewerGroup() {
		Preconditions.checkNotNull(config.getReviewerGroupName(), "ReviewerGroup can not be null");
		try {
            final AccountGroup reviewerGroup = findGroup(config.getReviewerGroupName());
            reviewerGroupId = reviewerGroup.getGroupUUID();
        } catch (OrmException e) {
            throw new IllegalStateException(String.format("Can not retrieve group: %s", config.getReviewerGroupName()));
        } catch (NoSuchGroupException e) {
            throw new IllegalStateException(String.format("Group doesn't exist: %s", config.getReviewerGroupName()));
        }
	}

    private String read(final File configFile) throws IOException {
        final Reader r = new InputStreamReader(new FileInputStream(configFile),
                "UTF-8");
        try {
            final StringBuilder buf = new StringBuilder();
            final char[] tmp = new char[1024];
            int n;
            while (0 < (n = r.read(tmp))) {
                buf.append(tmp, 0, n);
            }
            return buf.toString();
        } finally {
            r.close();
        }
    }

    private AccountGroup findGroup(final String name) throws OrmException, NoSuchGroupException {
        final AccountGroup g = groupCache.get(new AccountGroup.NameKey(name));
        if (g == null) {
            throw new NoSuchGroupException(name);
        }
        return g;
    }

}
