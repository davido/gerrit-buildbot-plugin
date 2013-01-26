/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.config;

import java.util.List;

import org.libreoffice.ci.gerrit.buildbot.publisher.LogPublisher;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class BuildbotConfig {

    private String email;
    private String logDir;
    private ImmutableList<BuildbotProject> projects;
    private boolean forgeReviewerIdentity;

    private boolean externalLogViewer;
    private String externalLogViewerHost;
    private String externalLogViewerJob;
    private String externalLogViewerUrl;
    private LogPublisher publisher;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public void setProjects(ImmutableList<BuildbotProject> projects) {
        this.projects = projects;
    }

    public List<BuildbotProject> getProjects() {
        return projects;
    }

    public BuildbotProject findProject(String name) {
        Preconditions.checkNotNull(name, "name must not be null");
        for (BuildbotProject p : projects) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public boolean isProjectSupported(String name) {
        return findProject(name) == null ? false : true;
    }

    public void setForgeReviewerIdentity(boolean forge) {
        this.forgeReviewerIdentity = forge;
    }

    public boolean isForgeReviewerIdentity() {
        return this.forgeReviewerIdentity;
    }

    public boolean isExternalLogViewer() {
        return externalLogViewer;
    }

    public void setExternalLogViewer(boolean externalLogViewer) {
        this.externalLogViewer = externalLogViewer;
    }

    public String getExternalLogViewerHost() {
        return externalLogViewerHost;
    }

    public void setExternalLogViewerHost(String externalLogViewerHost) {
        this.externalLogViewerHost = externalLogViewerHost;
    }

    public String getExternalLogViewerJob() {
        return externalLogViewerJob;
    }

    public void setExternalLogViewerJob(String externalLogViewerJob) {
        this.externalLogViewerJob = externalLogViewerJob;
    }

    public String getExternalLogViewerUrl() {
        return externalLogViewerUrl;
    }

    public void setExternalLogViewerUrl(String externalLogViewerUrl) {
        this.externalLogViewerUrl = externalLogViewerUrl;
    }

    public void setlogPublisher(LogPublisher publisher) {
        this.setPublisher(publisher);
    }

    public LogPublisher getPublisher() {
        return publisher;
    }

    public void setPublisher(LogPublisher publisher) {
        this.publisher = publisher;
    }
}

