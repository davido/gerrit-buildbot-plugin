package org.libreoffice.ci.gerrit.buildbot.commands;

public enum TaskStatus {
	SUCCESS,
	FAILED,
	CANCELED,
	CANCELLED,
	DISCARDED;

	public boolean isSuccess() {
		return this == SUCCESS;
	}
	
	public boolean isCancelled() {
		return this == CANCELED || this == CANCELLED;
	}

	public boolean isFailed() {
		return this == FAILED;
	}
	
	public boolean isDiscarded() {
		return this == DISCARDED;
	}
}
