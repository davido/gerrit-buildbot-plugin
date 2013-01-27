/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.StreamCopyThread;
import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsLogPublisher implements LogPublisher {
    static final Logger log = LoggerFactory
            .getLogger(JenkinsLogPublisher.class);

    final static String JENKINS_SSH_COMMAND = "set-external-build-result";
    final static String JENKINS_TEST_CHANNEL = "who-am-i";

    @Override
    public String publishLog(BuildbotConfig config, String ticket,
            String boxId, TaskStatus status, InputStream in) throws Exception {
        final String cmd = JENKINS_SSH_COMMAND + " --display " + ticket + "_" + boxId
                + " --job " + config.getExternalLogViewerJob() + " --result "
                + (status.isSuccess() ? "0" : "1") + " --log -";
        OutputStream errStream = newErrorBufferStream();
        OutputStream outStream = newErrorBufferStream();
        URIish jenkins = null;
        int exitValue = -1;
        try {
            jenkins = new URIish().setHost(config.getExternalLogViewerHost());
            RemoteSession ssh = connect(jenkins);
            Process proc = ssh.exec(cmd, 0/*timeout*/);
            StreamCopyThread out = new StreamCopyThread(proc.getInputStream(),
                    outStream);
            StreamCopyThread err = new StreamCopyThread(proc.getErrorStream(),
                    errStream);
            StreamCopyThread inp = new StreamCopyThread(in, 
                    proc.getOutputStream());
            out.start();
            err.start();
            inp.start();
            try {
                out.flush();
                err.flush();
                inp.flush();
                proc.waitFor();
                exitValue = proc.exitValue();
                out.halt();
                err.halt();
                inp.halt();
            } catch (InterruptedException interrupted) {
                log.error("process interupted: ", interrupted);
            }
            ssh.disconnect();
        } catch (IOException e) {
            log.error(
                    String.format("Error pushing log to %s:\n"
                            + "  Exception: %s\n" + " Command: %s\n"
                            + "  Output: %s", jenkins, e, cmd, errStream), e);
            return null;
        }
        String result = String.format("%s/job/%s/%d", config.getExternalLogViewerUrl(),
                config.getExternalLogViewerJob(), exitValue);
        log.debug("log url: {}", result);
        return result;
    }

    @Override
    public String testChannel(BuildbotConfig config) {
        final String cmd = JENKINS_TEST_CHANNEL;
        StringBuilder builder = new StringBuilder(cmd.length())
                .append(">ssh ").append(config.getExternalLogViewerHost())
                .append(" ").append(cmd).append("\n");
        OutputStream errStream = newErrorBufferStream();
        OutputStream outStream = newErrorBufferStream();
        URIish jenkins = null;
        try {
            jenkins = new URIish().setHost(config.getExternalLogViewerHost());
            RemoteSession ssh = connect(jenkins);
            Process proc = ssh.exec(cmd, 0/*timeout*/);
            StreamCopyThread out = new StreamCopyThread(proc.getInputStream(),
                    outStream);
            StreamCopyThread err = new StreamCopyThread(proc.getErrorStream(),
                    errStream);
            out.start();
            err.start();
            try {
                out.flush();
                err.flush();
                proc.waitFor();
                proc.exitValue();
                out.halt();
                err.halt();
            } catch (InterruptedException interrupted) {
                log.error("process interupted: ", interrupted);
            }
            ssh.disconnect();
            return builder.append("<").append(outStream.toString()).toString();
        } catch (IOException e) {
            log.error(
                    String.format("Error testing log channel\n"
                            + "  Exception: %s\n" + " Command: %s\n"
                            + "  Output: %s", jenkins, e, cmd, errStream), e);
            return null;
        }
    }

    private static RemoteSession connect(URIish uri) throws TransportException {
        return SshSessionFactory.getInstance().getSession(uri, null,
                FS.DETECTED, 0);
    }

    private static OutputStream newErrorBufferStream() {
        return new OutputStream() {
            private final StringBuilder out = new StringBuilder();
            private final StringBuilder line = new StringBuilder();

            @Override
            public synchronized String toString() {
                while (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
                    out.setLength(out.length() - 1);
                }
                return out.toString();
            }

            @Override
            public synchronized void write(final int b) {
                if (b == '\r') {
                    return;
                }

                line.append((char) b);

                if (b == '\n') {
                    out.append(line);
                    line.setLength(0);
                }
            }
        };
    }
}
