/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.ci.gerrit.buildbot.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Set;

import com.google.common.collect.Lists;

public class TBBlockingQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    Platform platform;
    private final LinkedList<BuildbotPlatformJob> queue = Lists.newLinkedList();

    public TBBlockingQueue(Platform platform) {
        this.platform = platform;
    }

    public void add(BuildbotPlatformJob tbJob) {
        synchronized (queue) {
            queue.add(tbJob);
        }
    }

    public BuildbotPlatformJob poll(Set<String> branchSet) {
        synchronized (queue) {
            if (branchSet.isEmpty()) {
                return queue.poll();
            }
            for (int i = 0; i < queue.size(); i++) {
                if (branchSet.contains(queue.get(i).getParent()
                        .getGerritBranch())) {
                    return queue.remove(i);
                }
            }
            return null;
        }
    }

    public BuildbotPlatformJob peek(Set<String> branchSet) {
        synchronized (queue) {
            if (branchSet.isEmpty()) {
                return queue.peek();
            }
            for (int i = 0; i < queue.size(); i++) {
                if (branchSet.contains(queue.get(i).getParent()
                        .getGerritBranch())) {
                    return queue.get(i);
                }
            }
            return null;
        }
    }

    public boolean remove(BuildbotPlatformJob job) {
        synchronized (queue) {
            return queue.remove(job);
        }
    }
    
    public boolean isEmpty() {
        synchronized (queue) {
            return queue.isEmpty();
        }
    }

}
