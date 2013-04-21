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

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfigProvider;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControlProvider;
import org.libreoffice.ci.gerrit.buildbot.publisher.BuildbotLogPublisher;
import org.libreoffice.ci.gerrit.buildbot.publisher.JenkinsLogPublisher;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.inject.AbstractModule;
import com.google.inject.internal.UniqueAnnotations;

class BuildbotModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BuildbotConfig.class).toProvider(BuildbotConfigProvider.class).in(SINGLETON);
        bind(BuildbotLogicControl.class).toProvider(BuildbotLogicControlProvider.class).in(SINGLETON);
        bind(AllProjectsName.class).toProvider(AllProjectsNameProvider.class);
        bind(BuildbotLogPublisher.class).in(SINGLETON);
        bind(JenkinsLogPublisher.class).in(SINGLETON);
        bind(StreamEventPipeline.class).in(SINGLETON);
        bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create())
        .to(StreamEventPipeline.class);
    }
}
