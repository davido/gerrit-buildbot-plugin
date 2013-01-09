package org.libreoffice.ci.gerrit.buildbot.config;

public enum TriggerStrategie {

	/**
	 * patch set created event
	 */
	PATCHSET_CREATED,
	/**
	 * a proposed patch get build if and only if: 
	 * 
	 * it has no review < 0, 
	 * it has no verify < 0,
	 * it has no verify > 0,
	 * it has at least 1 review > 0 from a user that belong to the `Reviewer` Group and
	 * the build is not triggered already
	 */
	POSITIVE_REVIEW,
	
	/**
	 * per schedule ssh command
	 * 
	 */
	MANUALLY
}
