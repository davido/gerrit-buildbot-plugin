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
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jgit.lib.Config;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.LogicControl;
import org.libreoffice.ci.gerrit.buildbot.logic.LogicControlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.ChangeHooks;
import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;

class BuildbotModule extends AbstractModule {
	static final Logger log = LoggerFactory.getLogger(BuildbotModule.class);
	private final static String BUILDBOT_CONFIG_RESOURCE = "buildbot.config";

	@Inject
	private ChangeHooks hooks;

	IdentifiedUser user;

	@Inject
	private IdentifiedUser.GenericFactory identifiedUserFactory;

	@Inject
	private AccountByEmailCache byEmailCache;

	@Inject
	@PluginData
	private java.io.File pluginDataDir;

	@Inject
	private Injector creatingInjector;
	
	private LogicControl control;
	
	private BuildbotConfig config;
	
//	@Inject
//	GerritConfig gerritConfig;
	
	private final ChangeListener listener = new ChangeListener() {
		@Override
		public void onChangeEvent(final ChangeEvent event) {
			if (event instanceof PatchSetCreatedEvent) {
				PatchSetCreatedEvent patchSetCreatedEvent = (PatchSetCreatedEvent) event;
				log.debug("patch-set-created project: {}", patchSetCreatedEvent.change.project);
				if (!control.isProjectSupported(patchSetCreatedEvent.change.project)) {
					log.debug("skip event");
					return;
				}
				log.debug("dispatch event branch: {}, ref: {}", 
						patchSetCreatedEvent.change.branch,
						patchSetCreatedEvent.patchSet.ref);
				control.startGerritJob(patchSetCreatedEvent);
			}
		}
	};

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
		File configFile = new File(pluginDataDir, BUILDBOT_CONFIG_RESOURCE);
		try {
			cfg.fromText(read(configFile));
		} catch (Exception ex) {
			log.error(ex.getMessage());
			throw new IllegalStateException(String.format(
					"can not find config file: %s",
					configFile.getAbsolutePath()));
		}
		String email = cfg.getString("user", null, "mail");
		String project = cfg.getString("project", null, "name");
		String directory = cfg.getString("log", null, "directory");
		config = new BuildbotConfig();
		config.setEmail(email);
		config.setProject(project);
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
}
