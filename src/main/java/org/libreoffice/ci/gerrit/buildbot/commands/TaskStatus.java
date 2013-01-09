package org.libreoffice.ci.gerrit.buildbot.commands;

public enum TaskStatus {
	SUCCESS,
	FAILED,
	CANCELED,
	DISCARDED;

	public boolean isSuccess() {
		return this == SUCCESS;
	}
	
	public boolean isCanceled() {
		return this == CANCELED;
	}

	public boolean isFailed() {
		return this == FAILED;
	}
	
	public boolean isDiscarded() {
		return this == DISCARDED;
	}
}
