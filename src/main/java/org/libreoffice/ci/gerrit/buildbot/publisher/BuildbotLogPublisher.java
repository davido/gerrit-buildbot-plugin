/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class BuildbotLogPublisher implements LogPublisher {
    static final Logger log = LoggerFactory
            .getLogger(BuildbotLogPublisher.class);

    @Inject
    @CanonicalWebUrl
    @Nullable
    Provider<String> urlProvider;

    private final static String LOGFILE_SUFFIX = ".log";

    private final static String LOGFILE_SERVLET_SUFFIX = "plugins/buildbot/log?file=";

    @Override
    public String publishLog(BuildbotConfig config, String ticket,
            String boxId, TaskStatus status, InputStream in) {
        String urllog = ticket + LOGFILE_SUFFIX;
        try {
            String file = config.getLogDir() + File.separator + urllog;
            int sChunk = 8192;
            GZIPInputStream zipin = new GZIPInputStream(in);
            byte[] buffer = new byte[sChunk];
            FileOutputStream out = new FileOutputStream(file);
            int length;
            while ((length = zipin.read(buffer, 0, sChunk)) != -1)
                out.write(buffer, 0, length);
            out.flush();
            out.close();
            zipin.close();
            in.close();
        } catch (Exception e) {
            log.error("can not save log file: ", e);
            return null;
        }
        return urlProvider.get() + LOGFILE_SERVLET_SUFFIX + urllog;
    }

    @Override
    public String testChannel(BuildbotConfig config) {
        throw new IllegalStateException("not implemented!");
    }
}
