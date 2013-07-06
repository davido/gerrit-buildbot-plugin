/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.webui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.TopMenuExtension;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class BuildbotTopMenu implements TopMenuExtension {
  private final List<MenuEntry> fullMenuEntries;
  private final List<MenuEntry> restrictedMenuEntries;
  private final Provider<CurrentUser> userProvider;
  private final BuildbotConfig config;

  @Inject
  public BuildbotTopMenu(@PluginName String pluginName,
      Provider<CurrentUser> userProvider, BuildbotConfig config) {
    this.userProvider = userProvider;
    this.config = config;
    String buildbotBaseUrl = "/plugins/" + pluginName + "/";
    List<MenuItem> restrictedItems =
        Arrays.asList(new MenuItem("Documentation", buildbotBaseUrl));
    this.restrictedMenuEntries =
        Arrays.asList(new MenuEntry("Buildbot", restrictedItems));
    List<MenuItem> fullItems = new ArrayList<MenuItem>(restrictedItems);
    fullItems.addAll(Arrays.asList(new MenuItem("Queue", buildbotBaseUrl
        + "queue/")));
    this.fullMenuEntries = Arrays.asList(new MenuEntry("Buildbot", fullItems));
  }

  @Override
  public List<MenuEntry> getEntries() {
    CurrentUser user = userProvider.get();
    if (user instanceof AnonymousUser) {
      return restrictedMenuEntries;
    } else {
      return (user.getEffectiveGroups()
          .contains(config.getBuildbotUserGroupId())
          ||
          user.getEffectiveGroups()
          .contains(config.getBuildbotAdminGroupId()))
          ? fullMenuEntries
          : restrictedMenuEntries;
    }
  }
}