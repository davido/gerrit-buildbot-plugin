package org.libreoffice.ci.gerrit.buildbot.commands;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.StreamCopyThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsJobScheduler {
    static final Logger log = LoggerFactory
            .getLogger(JenkinsJobScheduler.class);

    private static void createRemoteSsh(URIish uri) {
        // String quotedPath = QuotedString.BOURNE.quote(uri.getPath());
        String cmd = "set-external-build-result --display LINUX_12345 --job buildbot-master --result 0 --log url";
        // "&& git init --bare" + "&& git symbolic-ref HEAD ";
        // QuotedString.BOURNE.quote(head);
        OutputStream errStream = newErrorBufferStream();
        OutputStream outStream = newErrorBufferStream();
        try {
            RemoteSession ssh = connect(uri);
            Process proc = ssh.exec(cmd, 0);
            proc.getOutputStream().close();

            StreamCopyThread out = new StreamCopyThread(proc.getInputStream(),
                    outStream);
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

            System.out.println("Error:");
            System.out.println(errStream.toString());
            System.out.println("\nOutput:");
            System.out.println(outStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
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

    public static InputStream getStream() throws Exception {
        File file = new File("C:/users/david/projects/foo/log.gz");
        return new BufferedInputStream(new FileInputStream(file));
    }

    public static void main(String[] args) throws Exception {
        /*
         * URIish jenkins = new URIish();
         * 
         * jenkins = jenkins.setHost("idaia.de"); jenkins =
         * jenkins.setPort(38844); jenkins = jenkins.setScheme("ssh"); jenkins =
         * jenkins.setUser("david");
         * 
         * createRemoteSsh(jenkins);
         */
        
        short sChunk = 8192;
        InputStream in = getStream();
        GZIPInputStream zipin = new GZIPInputStream(in);
        byte[] buffer = new byte[sChunk];
        FileOutputStream out = new FileOutputStream(new File("C:/users/david/projects/foo/log.txt"));
        int length;
       while ((length = zipin.read(buffer, 0, sChunk)) != -1) {
//           System.out.println(buffer);
           out.write(buffer, 0, length);
       }
        out.flush();
        out.close();
        zipin.close();
        in.close();
    }
}
