package org.libreoffice.ci.gerrit.buildbot.publisher;

import java.io.InputStream;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;

public interface LogPublisher {

    public String publishLog(BuildbotConfig config, String ticket,
            String boxId, TaskStatus status, InputStream in) throws Exception;
}
