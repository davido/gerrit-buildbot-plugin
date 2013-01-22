/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotVersion;

import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;

@RequiresCapability(GlobalCapability.VIEW_QUEUE)
public final class VersionCommand extends BuildbotSshCommand {

  protected String getDescription() {
    return "Display buildbot version";
  }

  @Override
  protected void doRun() throws Failure {
    String v = BuildbotVersion.getVersion();
    if (v == null) {
      throw new Failure(1, "fatal: version unavailable");
    }

    stdout.println("buildbot version " + v);
  }
}
