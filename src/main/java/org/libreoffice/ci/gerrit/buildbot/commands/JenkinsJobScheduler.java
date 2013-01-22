package org.libreoffice.ci.gerrit.buildbot.commands;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.QuotedString;
import org.eclipse.jgit.util.io.StreamCopyThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsJobScheduler {
    static final Logger log = LoggerFactory.getLogger(PutCommand.class);

    private static void createRemoteSsh(URIish uri, String head) {
        String quotedPath = QuotedString.BOURNE.quote(uri.getPath());
        String cmd = "mkdir -p " + quotedPath + "&& cd " + quotedPath
                + "&& git init --bare" + "&& git symbolic-ref HEAD "
                + QuotedString.BOURNE.quote(head);
        OutputStream errStream = newErrorBufferStream();
        try {
            RemoteSession ssh = connect(uri);
            Process proc = ssh.exec(cmd, 0);
            proc.getOutputStream().close();
            StreamCopyThread out = new StreamCopyThread(proc.getInputStream(),
                    errStream);
            StreamCopyThread err = new StreamCopyThread(proc.getErrorStream(),
                    errStream);
            out.start();
            err.start();
            try {
                proc.waitFor();
                out.halt();
                err.halt();
            } catch (InterruptedException interrupted) {
                // Don't wait, drop out immediately.
            }
            ssh.disconnect();
        } catch (IOException e) {
            log.error(
                    String.format("Error creating remote repository at %s:\n"
                            + "  Exception: %s\n" + "  Command: %s\n"
                            + "  Output: %s", uri, e, cmd, errStream), e);
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
