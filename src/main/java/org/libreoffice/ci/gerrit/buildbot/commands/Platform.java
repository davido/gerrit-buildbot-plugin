/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.commands;

import org.libreoffice.ci.gerrit.buildbot.model.Os;

/**
 * @author davido
 * @deprecated use {link Os} instead
 */
enum Platform {
    WINDOWS, LINUX, MAC;

    public Os toOs() {
        switch (this) {
        case WINDOWS:
            return Os.Windows;
        case LINUX:
            return Os.Linux;
        case MAC:
        default:
            return Os.MacOSX;
        }
    }
}
