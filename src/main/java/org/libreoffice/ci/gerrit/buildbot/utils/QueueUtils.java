package org.libreoffice.ci.gerrit.buildbot.utils;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.libreoffice.ci.gerrit.buildbot.commands.TaskType;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.Os;
import org.libreoffice.ci.gerrit.buildbot.model.TBBlockingQueue;
import org.libreoffice.ci.gerrit.buildbot.model.Ticket;

public class QueueUtils {

    public static void dumpQueue(PrintWriter stdout, TaskType type,
            BuildbotLogicControl control, String project, boolean dump) {
        stdout.print("----------------------------------------------"
                + "--------------------------------\n");
        stdout.print(String.format("%-17s %-12s %-12s %-22s %-3s %s\n", //
                "Task-Id", "Start/End", "Type/State", "Ref", "Bot", "Branch"));
        int numberOfPendingTasks = 0;
        List<GerritJob> changes = control.getGerritJobs(project);
        synchronized (changes) {
            for (GerritJob change : changes) {
                if (type == null || type.equals(TaskType.CHANGE)) {
                    numberOfPendingTasks++;
                    stdout.print(String.format(
                            "%-17s %-12s %-12s %-22s %-3s %s\n", //
                            change.getId(), time(change.getStartTime(), 0),
                            "Change", change.getGerritRef(),
                            "-",
                            change.getGerritBranch()));
                }
                if (type == null || type.equals(TaskType.JOB)) {
                    List<BuildbotPlatformJob> list = change.getBuildbotList();
                    synchronized (list) {
                        numberOfPendingTasks = dumpTasks(stdout,
                                numberOfPendingTasks, list);
                    }
                }
            }
        }
        stdout.print("----------------------------------------------"
                + "--------------------------------\n");
        stdout.print("  " + numberOfPendingTasks + " task(s)\n");
        
        if (dump) {
            numberOfPendingTasks = 0;
            stdout.print("Verbose\n");
            stdout.print("----------------------------------------------"
                    + "--------------------------------\n");
            Map<Os, TBBlockingQueue> map = 
                    control.getTBQueueMap(project);
            synchronized (map) {
                for (Os p : Os.values()) {
                    TBBlockingQueue queue = map.get(p);
                    stdout.print("Queue for platform: " + p.name() + "\n"); 
                    numberOfPendingTasks += queue.dumpTasks(stdout);
                }
            }
            stdout.print("----------------------------------------------"
                    + "--------------------------------\n");
            stdout.print("  " + numberOfPendingTasks + " task(s)\n");
        }
    }

    public static int dumpTasks(PrintWriter stdout, int numberOfPendingTasks,
            List<BuildbotPlatformJob> list) {
        for (BuildbotPlatformJob job : list) {
            String jobId = job.getParent().getId() + "_"
                    + job.getPlatformString();
            String time = "-";
            Ticket t = job.getTicket();
            String status = "Job: ";
            if (job.getResult() != null && job.getResult().getStatus().isDiscarded()) {
                status += "DISCARDED";
            } else if (!job.isStarted()) {
                status += "INIT";
            } else if (job.isReady()) {
                status += job.getResult().getStatus().name();
                time = time(job.getResult().getEndTime(), 0);
            } else {
                status += "STARTED";
                time = time(t.getStartTime(), 0);
            }
            stdout.print(String.format(
                    "%-17s %-12s %-12s %-22s %-3s %s\n", //
                    jobId, time, status, job.getParent()
                            .getGerritRef(),
                            job.getTinderboxId() == null ? "-" : job.getTinderboxId(),
                            job.getParent()
                            .getGerritBranch()));
            numberOfPendingTasks++;
        }
        return numberOfPendingTasks;
    }

    private static String time(final long now, final long delay) {
        final Date when = new Date(now + delay);
        return new SimpleDateFormat("MMM-dd HH:mm").format(when);
    }

}
