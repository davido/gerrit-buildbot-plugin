package org.libreoffice.ci.gerrit.buildbot.logic;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class BuildbotLogicControlProvider implements Provider<BuildbotLogicControl> {

	BuildbotConfig config;

	@Inject
	BuildbotLogicControlProvider(BuildbotConfig config) {
		this.config = config;
	}

	public BuildbotLogicControl get() {
		return new BuildbotLogicControl(config);
	}
}
