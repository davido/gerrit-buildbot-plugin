package org.libreoffice.ci.gerrit.buildbot.commands;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.change.PostReview;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.project.ChangeControl;
import com.google.gerrit.server.project.NoSuchChangeException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public abstract class BuildbotSshCommand extends SshCommand {

	@Inject
	protected BuildbotLogicControl control;

	@Inject
	protected BuildbotConfig config;

	@Inject
	protected ReviewDb db;

	@Inject
	protected Provider<PostReview> reviewProvider;

	@Inject
	protected ChangeControl.Factory changeControlFactory;
    
	@Override
    protected final void run() throws UnloggedFailure, OrmException, Failure {
	    doRun();
	}

	protected abstract void doRun() throws UnloggedFailure, OrmException, Failure;
	
	protected void approveOne(final PatchSet.Id patchSetId,
			final String changeComment, final String labelName,
			final short value) throws Exception {
		PostReview.Input review = createReview(changeComment);
		review.labels.put(labelName, value);
		applyReview(patchSetId, review);
	}
	protected void approveOne(final PatchSet.Id patchSetId,
			final String changeComment, final List<ApproveOption> optionList) throws Exception {
	    PostReview.Input review = createReview(changeComment);
	    for (ApproveOption ao : optionList) {
	        Short v = ao.value();
	        if (v != null) {
	          review.labels.put(ao.getLabelName(), v);
	        }
	    }
		applyReview(patchSetId, review);
	}

	private void applyReview(final PatchSet.Id patchSetId,
			PostReview.Input review) throws NoSuchChangeException,
			OrmException, NoSuchFieldException, IllegalAccessException,
			AuthException, BadRequestException {
		ChangeControl ctl = changeControlFactory.controlFor(patchSetId
				.getParentKey());
		if (!review.labels.isEmpty()) {
			RevisionResource rsrc = new RevisionResource(
					new ChangeResource(ctl), db.patchSets().get(patchSetId));
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

	private void forgeReviewerIdentity(RevisionResource rsrc)
			throws NoSuchFieldException, IllegalAccessException {
		if (config.isForgeReviewerIdentity()) {
			// Replace current user with buildbot user
			Field field = ProjectControl.class.getDeclaredField("user");
			field.setAccessible(true);
			field.set(rsrc.getControl().getProjectControl(),
					control.getBuildbot());
		}
	}

    protected static String time(final long now, final long delay) {
        final Date when = new Date(now + delay);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }

	protected void writeError(final String msg) {
		try {
			err.write(msg.getBytes(ENC));
		} catch (IOException e) {
		}
	}

}
