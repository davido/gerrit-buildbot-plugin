/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot;

import org.libreoffice.ci.gerrit.buildbot.servlets.LogfileServlet;
import org.libreoffice.ci.gerrit.buildbot.servlets.QueueServlet;
import org.libreoffice.ci.gerrit.buildbot.webui.BuildbotTopMenu;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.TopMenuExtension;
import com.google.gerrit.httpd.plugins.HttpPluginModule;

class HttpBuildbotModule extends HttpPluginModule {
  @Override
  protected void configureServlets() {
	  serve("/log").with(LogfileServlet.class);
	  serve("/queue").with(QueueServlet.class);
	  serve("/queue/*").with(QueueServlet.class);
      DynamicSet.bind(binder(), TopMenuExtension.class)
          .to(BuildbotTopMenu.class);
  }
}
