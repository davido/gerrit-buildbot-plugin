package org.libreoffice.ci.gerrit.buildbot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.config.TriggerStrategy;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.ChangeHooks;
import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;

public class StreamEventPipeline implements LifecycleListener {

	static final Logger log = LoggerFactory.getLogger(StreamEventPipeline.class);
	
    @Inject
    private ChangeHooks hooks;

    IdentifiedUser buildbot;

    @Inject
    SchemaFactory<ReviewDb> schema;

    @Inject
    private IdentifiedUser.GenericFactory identifiedUserFactory;

    @Inject
    private AccountByEmailCache byEmailCache;
    
    @Inject
    BuildbotConfig config;

    @Inject
    private SitePaths site;
    
    @Inject
    BuildbotLogicControl control;

    @Override
    public void stop() {
    	hooks.removeChangeListener(listener);
    	control.stop();
    }

    @Override
    public void start() {
        Set<Account.Id> ids = byEmailCache.get(config.getEmail());
        if (CollectionUtils.isEmpty(ids)) {
            throw new IllegalStateException("user not found for email: "
                    + config.getEmail());
        }
        Account.Id id = ids.iterator().next();
        buildbot = identifiedUserFactory.create(id);
        control.setBuildbot(buildbot);
        hooks.addChangeListener(listener, buildbot);
        control.start();
    }

    private final ChangeListener listener = new ChangeListener() {
        @Override
        public void onChangeEvent(final ChangeEvent event) {
        	
            if (event instanceof PatchSetCreatedEvent) {
                PatchSetCreatedEvent patchSetCreatedEvent = (PatchSetCreatedEvent) event;
                log.debug("patch-set-created project: {}", patchSetCreatedEvent.change.project);
                if (!config.isProjectSupported(patchSetCreatedEvent.change.project)) {
                    log.debug("skip event: buildbot is not activated for project: {} ", patchSetCreatedEvent.change.project);
                    return;
                }
                BuildbotProject p = config.findProject(patchSetCreatedEvent.change.project);

                if (TriggerStrategy.PATCHSET_CREATED != p.getTriggerStrategy()) {
                    log.debug("skip event: non PATCHSET_CREATED trigger strategie for project: {} ", 
                            patchSetCreatedEvent.change.project);
                    synchronized (control) {
                        GerritJob job = control.findJobByChange(
                                patchSetCreatedEvent.change.project, 
                                patchSetCreatedEvent.change.id);
                        if (job == null) {
                            return;
                        }
                        control.handleStaleJob(patchSetCreatedEvent.change.project, job);
                    }
                    return;
                }
                log.debug("dispatch event branch: {}, ref: {}",
                        patchSetCreatedEvent.change.branch,
                        patchSetCreatedEvent.patchSet.ref);
                control.startGerritJob(patchSetCreatedEvent);
            } else if (event instanceof CommentAddedEvent) {
                CommentAddedEvent commentAddedEvent = (CommentAddedEvent) event;
                if (!config.isProjectSupported(commentAddedEvent.change.project)) {
                    log.debug("skip event: buildbot is not activated for project: {} ", commentAddedEvent.change.project);
                    return;
                }
                BuildbotProject p = config.findProject(commentAddedEvent.change.project);

                if (TriggerStrategy.POSITIVE_REVIEW != p.getTriggerStrategy()) {
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
            if (eventAuthor.getAccountId().equals(buildbot.getAccountId())) {
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
        GerritJob job = control.findJobByRevision(commentAddedEvent.change.project, 
        		commentAddedEvent.patchSet.revision);
        if (job != null) {
            log.debug("ignore this comment: build job was already scheduled at {}", job.getStartTime());
            return false;
        }

        BuildbotProject p = config.findProject(commentAddedEvent.change.project);
        // check branch if configured
        if (!p.getBranches().isEmpty()) {
        	if (!p.getBranches().contains(commentAddedEvent.change.branch)) {
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
        checkApprovals(p, list, res);

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

    private void checkApprovals(BuildbotProject p, Collection<PatchSetApproval> approvals,
            List<PositiveReviewResultEntry> res) {
        for (PatchSetApproval attr : approvals) {
            PositiveReviewResultEntry r = new PositiveReviewResultEntry();

            final Account.Id id = attr.getAccountId();
            IdentifiedUser eventAuthor = identifiedUserFactory.create(id);

            log.debug("check approval from user: {}", eventAuthor.getUserName());
            GroupMembership members = eventAuthor.getEffectiveGroups();
            if (members.contains(p.getReviewerGroupId())) {
                r.approvedByMemberOfGroup = true;
                log.debug("user {} is member of Reviewer group", eventAuthor.getUserName());
            }

            if ("Verified".equals(attr.getLabel())) {
            //buildbot-2.5-plugin
            //if (verified.getId().get().toString().equals(attr.getCategoryId().toString())) {
                short v = Short.valueOf(attr.getValue()).shortValue();
                if (v > 0) {
                    r.positiveVerify = true;
                } else if (v < 0) {
                    r.negativeVerify = true;
                }
            } else if ("Code-Review".equals(attr.getLabel())) {
            //buildbot-2.5-plugin
            //} else if (reviewed.getId().get().toString().equals(attr.getCategoryId().toString())) {
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

}
