/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.libreoffice.ci.gerrit.buildbot.publisher.BuildbotLogPublisher;
import org.libreoffice.ci.gerrit.buildbot.publisher.JenkinsLogPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gerrit.common.errors.NoSuchGroupException;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.SitePaths;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class BuildbotConfigProvider implements Provider<BuildbotConfig> {

    static final Logger log = LoggerFactory
            .getLogger(BuildbotConfigProvider.class);

    private final static String SECTION_USER = "user";
    private static final String KEY_MAIL = "mail";
    private static final String KEY_FORGE_REVIEWER_IDENTITY = "forgeReviewerIdentity";
    private static final String KEY_BUILDBOT_ADMIN_GROUP_NAME = "buildbotAdminGroupName";

    private final static String SECTION_LOG = "log";
    private final static String KEY_MODE = "mode";
    private static final String KEY_DIRECTORY = "directory";
    private final static String KEY_HOST = "host";
    private final static String KEY_JOB = "job";
    private final static String KEY_URL = "url";

    private final static String SECTION_PROJECT = "project";
    private final static String KEY_BRANCH = "branch";
    private final static String KEY_TRIGGER = "trigger";
    private final static String KEY_REVIEWER_GROUP_NAME = "reviewerGroupName";

    private SitePaths site;

    private GroupCache groupCache;

    private BuildbotLogPublisher buildbotLogPublisher;

    private JenkinsLogPublisher jenkinsLogPublisher;

    @Inject
    public BuildbotConfigProvider(SitePaths site, GroupCache groupCache,
            BuildbotLogPublisher buildbotLogPublisher,
            JenkinsLogPublisher jenkinsLogPublisher) {
        this.site = site;
        this.groupCache = groupCache;
        this.setBuildbotLogPublisher(buildbotLogPublisher);
        this.setJenkinsLogPublisher(jenkinsLogPublisher);
    }

    @Override
    public BuildbotConfig get() {
        BuildbotConfig config = new BuildbotConfig();
        File file = new File(site.etc_dir, "buildbot.config");
        FileBasedConfig cfg = new FileBasedConfig(file, FS.DETECTED);
        if (!cfg.getFile().exists()) {
            throw new IllegalStateException(String.format(
                    "can not find config file: %s", file.getAbsolutePath()));
        }

        if (cfg.getFile().length() == 0) {
            throw new IllegalStateException(String.format(
                    "empty config file: %s", file.getAbsolutePath()));
        }

        try {
            cfg.load();
        } catch (ConfigInvalidException e) {
            throw new IllegalStateException(String.format(
                    "config file %s is invalid: %s", cfg.getFile(),
                    e.getMessage()), e);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("cannot read %s: %s",
                    cfg.getFile(), e.getMessage()), e);
        }

        config = new BuildbotConfig();
        config.setEmail(cfg.getString(SECTION_USER, null, KEY_MAIL));
        config.setForgeReviewerIdentity(cfg.getBoolean(SECTION_USER, null,
                KEY_FORGE_REVIEWER_IDENTITY, true));
        String buildbotAdminGroupName = cfg.getString(SECTION_USER, null,
                KEY_BUILDBOT_ADMIN_GROUP_NAME);
        Preconditions.checkNotNull(buildbotAdminGroupName,
                "buildbotAdminGroupName must not be null");
        config.setBuildbotAdminGroupId(getGroup(buildbotAdminGroupName));

        String publisherModeStr = cfg.getString(SECTION_LOG, null, KEY_MODE);
        Preconditions.checkNotNull(publisherModeStr, "log.mode must not be null");
        LogPublisherMode publisherMode = LogPublisherMode.valueOf(publisherModeStr.toUpperCase());
        Preconditions.checkNotNull(publisherMode, " log.mode has wrong value");
        config.setLogDir(cfg.getString(SECTION_LOG, null, KEY_DIRECTORY));
        if (publisherMode == LogPublisherMode.INTERN) {
            config.setlogPublisher(buildbotLogPublisher);
        } else {
            config.setExternalLogViewer(true);
            config.setExternalLogViewerHost(cfg.getString(SECTION_LOG, null, KEY_HOST));
            config.setExternalLogViewerUrl(cfg.getString(SECTION_LOG, null, KEY_URL));
            config.setExternalLogViewerJob(cfg.getString(SECTION_LOG, null, KEY_JOB));
            config.setlogPublisher(jenkinsLogPublisher);
        }

        ImmutableList.Builder<BuildbotProject> dest = ImmutableList.builder();

        for (BuildbotProject p : allProjects(config, cfg)) {
            dest.add(p);
        }
        config.setProjects(dest.build());
        return config;
    }

    private List<BuildbotProject> allProjects(BuildbotConfig config, FileBasedConfig cfg) {
        Set<String> names = cfg.getSubsections(SECTION_PROJECT);
        List<BuildbotProject> result = Lists.newArrayListWithCapacity(names
                .size());
        for (String name : names) {
            result.add(parseProject(config, cfg, name));
        }
        return result;
    }

    private BuildbotProject parseProject(BuildbotConfig config, FileBasedConfig cfg, String name) {
        BuildbotProject p = new BuildbotProject(name);
        String[] branches = cfg
                .getStringList(SECTION_PROJECT, name, KEY_BRANCH);
        String strStrategie = cfg.getString(SECTION_PROJECT, name, KEY_TRIGGER);

        Preconditions.checkNotNull(strStrategie, "strategie must not be null");

        p.setBranches(branches);

        TriggerStrategie triggerStrategie = TriggerStrategie
                .valueOf(strStrategie.toUpperCase());
        Preconditions.checkNotNull(triggerStrategie,
                String.format("unknown strategie %s", strStrategie));

        p.setTriggerStrategie(triggerStrategie);
        if (triggerStrategie == TriggerStrategie.POSITIVE_REVIEW) {
            String reviewerGroupName = cfg.getString(SECTION_PROJECT, name,
                    KEY_REVIEWER_GROUP_NAME);
            Preconditions.checkNotNull(reviewerGroupName,
                    "reviewerGroupName must not be null");
            p.setReviewerGroupId(getGroup(reviewerGroupName));
        }

        String buildbotAdminGroupName = cfg.getString(SECTION_PROJECT, null, KEY_BUILDBOT_ADMIN_GROUP_NAME);
        if (buildbotAdminGroupName != null) {
            p.setBuildbotAdminGroupId(getGroup(buildbotAdminGroupName));
        } else {
            p.setBuildbotAdminGroupId(config.getBuildbotAdminGroupId());
        }
        return p;
    }

    private AccountGroup.UUID getGroup(String groupName) {
        Preconditions.checkNotNull(groupName,
                "Group name must not be null");
        try {
            return findGroup(groupName).getGroupUUID();
        } catch (OrmException e) {
            throw new IllegalStateException(String.format(
                    "Can not retrieve group: %s", groupName));
        } catch (NoSuchGroupException e) {
            throw new IllegalStateException(String.format(
                    "Group doesn't exist: %s", groupName));
        }
    }

    private AccountGroup findGroup(final String name) throws OrmException,
            NoSuchGroupException {
        final AccountGroup g = groupCache.get(new AccountGroup.NameKey(name));
        if (g == null) {
            throw new NoSuchGroupException(name);
        }
        return g;
    }

    public BuildbotLogPublisher getBuildbotLogPublisher() {
        return buildbotLogPublisher;
    }

    public void setBuildbotLogPublisher(
            BuildbotLogPublisher buildbotLogPublisher) {
        this.buildbotLogPublisher = buildbotLogPublisher;
    }

    public JenkinsLogPublisher getJenkinsLogPublisher() {
        return jenkinsLogPublisher;
    }

    public void setJenkinsLogPublisher(JenkinsLogPublisher jenkinsLogPublisher) {
        this.jenkinsLogPublisher = jenkinsLogPublisher;
    }
}
