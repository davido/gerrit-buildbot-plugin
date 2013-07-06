/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.servlets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Instant;
import org.libreoffice.ci.gerrit.buildbot.commands.TaskStatus;
import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;
import org.libreoffice.ci.gerrit.buildbot.logic.BuildbotLogicControl;
import org.libreoffice.ci.gerrit.buildbot.model.BuildbotPlatformJob;
import org.libreoffice.ci.gerrit.buildbot.model.GerritJob;
import org.libreoffice.ci.gerrit.buildbot.model.TbJobResult;
import org.libreoffice.ci.gerrit.buildbot.model.Ticket;
import org.libreoffice.ci.gerrit.buildbot.review.ReviewPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;

@Singleton
public class QueueServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(QueueServlet.class);

  private final BuildbotLogicControl control;
  private final BuildbotConfig config;
  private final ReviewPublisher publisher;
  private final Provider<CurrentUser> cu;

  private final Renderer renderer;

  @Inject
  public QueueServlet(BuildbotLogicControl control, BuildbotConfig config,
      ReviewPublisher publisher, Provider<CurrentUser> cu) {
    this.control = control;
    this.config = config;
    this.publisher = publisher;
    this.cu = cu;
    renderer = new DefaultRenderer(null, "Buildbot Queue");
  }

  @Override
  protected void doGet(final HttpServletRequest req,
      final HttpServletResponse rsp) throws IOException, ServletException {
    String action = req.getParameter("action");
    if (!Strings.isNullOrEmpty(action)) {
      try {
        performAction(action);
        rsp.sendRedirect("queue");
      } catch (OrmException exc) {
        log.error(String.format("Can not perform action: %s", action), exc);
      }
    }
    doPost(req, rsp);
  }

  private void performAction(String action) throws OrmException {
    if (action.indexOf("cancel-") != -1) {
      String ticket = action.substring(action.indexOf('-') + 1);
      String id = ticket.substring(0, ticket.indexOf('_'));
      String project = control.findProjectByTicket(ticket);
      if (project != null) {
        GerritJob job = control.findJobById(project, id);
        if (job != null) {
          BuildbotPlatformJob task = job.getTbJob(ticket);
          TbJobResult result =
              control.setResultPossible(ticket, task.getTinderboxId(),
                  TaskStatus.CANCELLED, null);
          if (result != null) {
            publisher.postResultToReview(result);
          }
        }
      }
    }
  }

  /**
   * Render data to HTML using Soy.
   * 
   * @param req in-progress request.
   * @param res in-progress response.
   * @param key key.
   * @param templateName Soy template name; must be in one of the template files
   *        defined in {@link Renderer#SOY_FILENAMES}.
   */
  protected void renderHtml(HttpServletRequest req, HttpServletResponse res,
      Renderer renderer, String templateName, Map<String, ?> soyData)
      throws IOException {
    try {
      res.setContentType(RenderType.HTML.getMimeType());
      res.setCharacterEncoding(Charsets.UTF_8.name());
      setCacheHeaders(req, res);
      res.setStatus(HttpServletResponse.SC_OK);
      renderer.render(res, templateName, soyData);
    } finally {
      // req.removeAttribute(DATA_ATTRIBUTE);
    }
  }

  protected void setCacheHeaders(HttpServletRequest req, HttpServletResponse res) {
    setNotCacheable(res);
  }

  static void setNotCacheable(HttpServletResponse res) {
    res.setHeader(HttpHeaders.CACHE_CONTROL,
        "no-cache, no-store, max-age=0, must-revalidate");
    res.setHeader(HttpHeaders.PRAGMA, "no-cache");
    res.setHeader(HttpHeaders.EXPIRES, "Fri, 01 Jan 1990 00:00:00 GMT");
    res.setDateHeader(HttpHeaders.DATE, new Instant().getMillis());
  }

  @Override
  protected void doPost(final HttpServletRequest req,
      final HttpServletResponse rsp) throws IOException, ServletException {
    Map<String, Object> allData = getData();
    renderHtml(req, rsp, renderer, "buildbot.queue", allData);
  }

  private Map<String, Object> getData() {
    final Map<String, Object> allData = Maps.newHashMapWithExpectedSize(8);
    allData.put("title", "Buildbot Queue");
    final SoyListData soyListData = new SoyListData();
    for (String project : control.getAllProjects()) {
      List<GerritJob> gerritJobs = control.getGerritJobs(project);
      synchronized (gerritJobs) {
        for (GerritJob job : gerritJobs) {
          boolean userIsAdmin = false;
          CurrentUser user = cu.get();
          if (user instanceof IdentifiedUser) {
            if (user.getEffectiveGroups().contains(
                config.findProject(job.getGerritProject())
                    .getBuildbotAdminGroupId())) {
              userIsAdmin = true;
            }
          }

          List<BuildbotPlatformJob> list = job.getBuildbotList();
          synchronized (list) {
            addTasks(soyListData, list, userIsAdmin);
          }
        }
      }
    }
    allData.put("jobs", soyListData);
    return allData;
  }

  // private void addDummyTask(SoyListData soyListData) {
  // SoyMapData task = new SoyMapData();
  // String status = "INIT";
  // String startTime = "-";
  // String endTime = "-";
  // task.put("href", new SoyListData());
  // task.put("taskid", "4711_Linux");
  // task.put("start", startTime);
  // task.put("end", endTime);
  // task.put("ref", "ref");
  // task.put("bot", "TB");
  // task.put("branch", "branch");
  // task.put("status", status);
  // task.put("actionUrl", "queue");
  // soyListData.add(task);
  // }

  private void addTasks(SoyListData soyListData,
      List<BuildbotPlatformJob> list, boolean userIsAdmin) {
    // check if the user has the ACL
    for (BuildbotPlatformJob job : list) {
      SoyMapData task = new SoyMapData();
      String jobId = job.getParent().getId() + "_" + job.getPlatformString();
      String startTime = "-";
      String endTime = "-";
      Ticket t = job.getTicket();
      String status;
      task.put("href", new SoyListData());
      SoyListData actions = new SoyListData();
      if (job.getResult() != null && job.getResult().getStatus().isDiscarded()) {
        status = "DISCARDED";
      } else if (!job.isStarted()) {
        status = "INIT";
      } else if (job.isReady()) {
        status = job.getResult().getStatus().name();
        startTime = time(t.getStartTime());
        endTime = time(job.getResult().getEndTime());
        if (!Strings.isNullOrEmpty(job.getResult().getLog())) {
          final SoyListData hrefData = new SoyListData();
          hrefData.add(job.getResult().getLog());
          task.put("href", hrefData);
        }
      } else {
        status = "STARTED";
        startTime = time(t.getStartTime());
        if (userIsAdmin) {
          addStartActions(actions);
        }
      }
      task.put("taskid", jobId);
      task.put("start", startTime);
      task.put("end", endTime);
      task.put("ref", job.getParent().getGerritRef());
      task.put("bot", job.getTinderboxId() == null ? "-" : job.getTinderboxId());
      task.put("branch", job.getParent().getGerritBranch());
      task.put("status", status);
      task.put("actions", actions);
      soyListData.add(task);
    }
  }

  private void addStartActions(SoyListData actions) {
    SoyMapData data = new SoyMapData();
    data.put("name", "Cancel");
    data.put("action", "cancel");
    data.put("img", "/plugins/buildbot/static/redNot.png");
    actions.add(data);
  }

  private static String time(final long now) {
    final Date when = new Date(now);
    return new SimpleDateFormat("MMM-dd HH:mm").format(when);
  }

}
