package org.libreoffice.ci.gerrit.buildbot.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class BuildbotConfigProvider implements Provider<BuildbotConfig> {

	static final Logger log = LoggerFactory.getLogger(BuildbotConfigProvider.class);
	
	private SitePaths site;
	@Inject
	public BuildbotConfigProvider(SitePaths site) {
		this.site = site;
	}
	
	@Override
	public BuildbotConfig get() {
		BuildbotConfig config = new BuildbotConfig();
        Config cfg = new Config();
        File configFile = null;
        String configContent = null;
        try {
            configFile = new File(site.etc_dir, "buildbot.config");
            configContent = read(configFile);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new IllegalStateException(String.format(
                    "can not find config file: %s",
                    configFile.getAbsolutePath()), ex);
        }

        try {
            cfg.fromText(configContent);
        } catch (ConfigInvalidException ex) {
            log.error(ex.getMessage());
            throw new IllegalStateException(String.format(
                    "can not parse config file: %s",
                    configFile.getAbsolutePath()), ex);
        }
        String email = cfg.getString("user", null, "mail");
        String project = cfg.getString("project", null, "name");
        String[] branches = cfg.getStringList("project", null, "branch");
        String strStrategie = cfg.getString("project", null, "trigger");

        Preconditions.checkNotNull(strStrategie, "strategie must not be null");

        String directory = cfg.getString("log", null, "directory");
        config = new BuildbotConfig();
        config.setEmail(email);
        config.setProject(project);
        config.setBranches(branches);

        TriggerStrategie triggerStrategie = TriggerStrategie.valueOf(strStrategie.toUpperCase());
        Preconditions.checkNotNull(triggerStrategie, String.format("unknown strategie %s", strStrategie));

        config.setTriggerStrategie(triggerStrategie);
        if (triggerStrategie == TriggerStrategie.POSITIVE_REVIEW) {
            String reviewerGroupName = cfg.getString("project", null, "reviewerGroupName");
            Preconditions.checkNotNull(reviewerGroupName, "reviewerGroupName must not be null");
            config.setReviewerGroupName(reviewerGroupName);
        }
        config.setLogDir(directory);

        return config;
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
