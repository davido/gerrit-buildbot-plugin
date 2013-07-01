package org.libreoffice.ci.gerrit.buildbot.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gerrit.reviewdb.client.AccountGroup;

public class BuildbotProject implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private TriggerStrategy triggerStrategy;
    private AccountGroup.UUID reviewerGroupId;
    private AccountGroup.UUID buildbotAdminGroupId;
    private AccountGroup.UUID buildbotUserGroupId;
    private List<String> branches = Lists.newArrayList();

    public BuildbotProject(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTriggerStrategy(TriggerStrategy triggerStrategy) {
        this.triggerStrategy = triggerStrategy;
    }

    public TriggerStrategy getTriggerStrategy() {
        return this.triggerStrategy;
    }

    public List<String> getBranches() {
        return branches;
    }

    public void setBranches(String[] branches) {
        this.branches = Arrays.asList(branches);
    }

    public AccountGroup.UUID getReviewerGroupId() {
        return reviewerGroupId;
    }

    public void setReviewerGroupId(AccountGroup.UUID reviewerGroupId) {
        this.reviewerGroupId = reviewerGroupId;
    }

    public AccountGroup.UUID getBuildbotAdminGroupId() {
        return buildbotAdminGroupId;
    }

    public void setBuildbotAdminGroupId(AccountGroup.UUID buildbotAdminGroupId) {
        this.buildbotAdminGroupId = buildbotAdminGroupId;
    }

    public AccountGroup.UUID getBuildbotUserGroupId() {
        return buildbotUserGroupId;
    }

    public void setBuildbotUserGroupId(AccountGroup.UUID buildbotUserGroupId) {
        this.buildbotUserGroupId = buildbotUserGroupId;
    }
}
