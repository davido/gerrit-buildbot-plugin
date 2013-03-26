package org.libreoffice.ci.gerrit.buildbot.logic;

import java.util.Set;

import junit.framework.TestCase;

import org.junit.Assert;
import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.config.TriggerStrategie;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

public class BuildbotLogicControlTest extends TestCase {

    BuildbotConfig config;
    BuildbotLogicControl control;
    static final String PROJECT = "FOO";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        config = new BuildbotConfig();
        config.setEmail("email@site");
        config.setForgeReviewerIdentity(true);
        ImmutableList.Builder<BuildbotProject> projects = ImmutableList.builder();
        BuildbotProject project = new BuildbotProject(PROJECT);
        project.setTriggerStrategie(TriggerStrategie.MANUALLY);
        projects.add(project);
        config.setProjects(projects.build());
        control = new BuildbotLogicControl(config);
        control.start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        control.stop();
    }

    public void testCompleteJob() {
        control.startGerritJob(PROJECT, "master", "4711", "abcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42");
        Assert.assertNotNull(job);
        TbJobResult result = control.setResultPossible(job.getTicket(), "42", TaskStatus.SUCCESS, "url");
        Assert.assertNotNull(result);
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42");
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, "42");
        Assert.assertNotNull(job);
        result = control.setResultPossible(job.getTicket(), "42", TaskStatus.SUCCESS, "url");
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, "42");
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.MAC, branchSet, "42");
        Assert.assertNotNull(job);
        Assert.assertFalse(result.getTbPlatformJob().getParent().allJobsReady());
        result = control.setResultPossible(job.getTicket(), "42", TaskStatus.SUCCESS, "url");
        Assert.assertTrue(result.getTbPlatformJob().getParent().allJobsReady());
    }
    
    public void testDiscardedTasks() {
        control.startGerritJob(PROJECT, "master", "4711", "abcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42");
        Assert.assertNotNull(job);
        TbJobResult result = control.setResultPossible(job.getTicket(), "42", TaskStatus.FAILED, "url");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getTbPlatformJob().getParent().allJobsReady());
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42");
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, "42");
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.MAC, branchSet, "42");
        Assert.assertNull(job);
    }

}
