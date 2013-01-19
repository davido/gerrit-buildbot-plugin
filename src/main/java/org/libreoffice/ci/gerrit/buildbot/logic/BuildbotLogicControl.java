package org.libreoffice.ci.gerrit.buildbot.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.logic.impl.ProjectControlImpl;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;

import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;

public class BuildbotLogicControl {

	private final Map<String, ProjectControl> projectMap = new ConcurrentHashMap<String, ProjectControl>();

	private BuildbotConfig config;

	private IdentifiedUser buildbot;

	public BuildbotLogicControl(BuildbotConfig config) {
		this.config = config;
	}

	public void start() {
		// init
		for (BuildbotProject buildbotProject : config.getProjects()) {
			projectMap.put(buildbotProject.getName(), new ProjectControlImpl());
		}
		for (Map.Entry<String, ProjectControl> entry : projectMap.entrySet()) {
			entry.getValue().start();
		}
	}

	public void stop() {
		synchronized (projectMap) {
			for (Map.Entry<String, ProjectControl> entry : projectMap.entrySet()) {
				entry.getValue().stop();
			}
			// release
			projectMap.clear();
		}
	}

	public TbJobDescriptor launchTbJob(String project, Platform platform,
			Set<String> branch, String box) {
		return projectMap.get(project).launchTbJob(platform, branch, box);
	}
	
	public TbJobResult setResultPossible(String ticket, String boxId, TaskStatus status, String log) {
		synchronized (projectMap) {			
			for (Map.Entry<String, ProjectControl> entry : projectMap.entrySet()) {
				TbJobResult result = entry.getValue().setResultPossible(ticket, boxId, status, log);
				if (result != null) {
					return result;
				}
			}
			return null;
		}
	}
	
	public List<GerritJob> getGerritJobs(String project) {
		return projectMap.get(project).getGerritJobs();
	}
	
	public GerritJob findJobByRevision(String project, String revision) {
		return projectMap.get(project).findJobByRevision(revision);
	}
	
	public void startGerritJob(String project, Change change, PatchSet patchSet) {
		projectMap.get(project).startGerritJob(change, patchSet);
	}
	
	public void startGerritJob(PatchSetCreatedEvent event) {
		projectMap.get(event.change.project).startGerritJob(event);
	}
	public void startGerritJob(CommentAddedEvent event) {
		projectMap.get(event.change.project).startGerritJob(event);
	}

	public IdentifiedUser getBuildbot() {
		return buildbot;
	}

	public void setBuildbot(IdentifiedUser buildbot) {
		this.buildbot = buildbot;
	}
}
