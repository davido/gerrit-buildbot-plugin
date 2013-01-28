package org.libreoffice.ci.gerrit.buildbot.logic;

import java.io.PrintWriter;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotProject;
import org.libreoffice.ci.gerrit.buildbot.config.TriggerStrategie;
import org.libreoffice.ci.gerrit.buildbot.model.Platform;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobDescriptor;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;
import org.libreoffice.ci.gerrit.buildbot.utils.QueueUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

public class BuildbotLogicControlTest {

    BuildbotConfig config;
    BuildbotLogicControl control;
    static final String PROJECT = "FOO";
    static final String TB1 = "42";
    static final String TB2 = "43";
    static final String URL = "url";
    
    @Before
    public void setUp() throws Exception {
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
        BasicConfigurator.configure();
    }

    @After
    public void tearDown() throws Exception {
        control.stop();
    }

    @Test()
    public void testCompleteJob() {
        control.startGerritJob(PROJECT, "master", "4711", "abcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, TB1, false);
        Assert.assertNotNull(job);
        TbJobResult result = control.setResultPossible(job.getTicket(), TB1, TaskStatus.SUCCESS, URL);
        Assert.assertNotNull(result);
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, TB1, false);
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, TB1, false);
        Assert.assertNotNull(job);
        result = control.setResultPossible(job.getTicket(), TB1, TaskStatus.SUCCESS, URL);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, TB1, false);
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.MAC, branchSet, TB1, false);
        Assert.assertNotNull(job);
        Assert.assertFalse(result.getTbPlatformJob().getParent().allJobsReady());
        result = control.setResultPossible(job.getTicket(), TB1, TaskStatus.SUCCESS, URL);
        Assert.assertTrue(result.getTbPlatformJob().getParent().allJobsReady());
        dumpQueue();
    }

    @Test()
    public void testBoxMismatch() {
        control.startGerritJob(PROJECT, "master", "4711", "abcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, TB1, true);
        Assert.assertNotNull(job);
        TbJobResult result = control.setResultPossible(job.getTicket(), TB2, TaskStatus.SUCCESS, URL);
        Assert.assertNull(result);
        dumpQueue();
    }

    @Test()
    public void testFindProjectByTicket() {
        control.startGerritJob(PROJECT, "master", "4711", "abcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, TB1, false);
        Assert.assertEquals(PROJECT, control.findProjectByTicket(job.getTicket()));
    }

    @Test
    public void test2DiscardedTasks() {
        control.startGerritJob(PROJECT, "master", "4711", "abcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", false);
        Assert.assertNotNull(job);
        TbJobResult result = control.setResultPossible(job.getTicket(), "42", TaskStatus.FAILED, "url");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getTbPlatformJob().getParent().allJobsReady());
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", false);
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, "42", false);
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.MAC, branchSet, "42", false);
        Assert.assertNull(job);
        dumpQueue();
    }

    @Test
    public void test1CanceledTasks() {
        control.startGerritJob(PROJECT, "master", "4711", "a1bcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", false);
        Assert.assertNotNull(job);
        TbJobResult result = control.setResultPossible(job.getTicket(), "42", TaskStatus.SUCCESS, "url");
        Assert.assertNotNull(result);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, TB1, false);
        Assert.assertNotNull(job);
        result = control.setResultPossible(job.getTicket(), TB1, TaskStatus.SUCCESS, URL);
        job = control.launchTbJob(PROJECT, Platform.MAC, branchSet, TB1, false);
        Assert.assertNotNull(job);
        result = control.setResultPossible(job.getTicket(), TB1, TaskStatus.CANCELED, URL);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.getTbPlatformJob().getParent().allJobsReady());
        dumpQueue();
    }

    @Test
    public void testPeekTasks() {
        control.startGerritJob(PROJECT, "master", "4711", "a1bcdefghijklmnopqrstuvwxyz");
        Set<String> branchSet = Sets.newHashSet();
        TbJobDescriptor job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", true);
        Assert.assertNotNull(job);
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", true);
        Assert.assertNotNull(job);
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", false);
        Assert.assertNotNull(job);
        job = control.launchTbJob(PROJECT, Platform.LINUX, branchSet, "42", false);
        Assert.assertNull(job);
        job = control.launchTbJob(PROJECT, Platform.WINDOWS, branchSet, TB1, true);
        Assert.assertNotNull(job);
        job = control.launchTbJob(PROJECT, Platform.MAC, branchSet, TB1, false);
        Assert.assertNotNull(job);
        dumpQueue();
    }

    @Test
    public void testManyJobs() {
        String prefix = "a1bcdefghi";
        Set<String> branchSet = Sets.newHashSet();
        for (int i = 0; i < 100; i++) {
            control.startGerritJob(PROJECT, "master", "4711", prefix + i);
        }
        for (int i = 0; i < 50; i++) {
            for (Platform p : Platform.values()) {
                TbJobDescriptor job = control.launchTbJob(PROJECT, p, branchSet, "42", false);
                Assert.assertNotNull(job);
            }
        }
        dumpQueue();
    }

    private void dumpQueue() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PrintWriter stdout = new PrintWriter(System.out);
        QueueUtils.dumpQueue(stdout, null, control, PROJECT, true);
        stdout.flush();
    }
}
