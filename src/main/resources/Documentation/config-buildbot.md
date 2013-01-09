Buildbot Plugin - Configuration
===============================

This plugin is a multi project and multi platform queue manager
for patch verification.

File `buildbot.config`
------------------------

The mandatory file `'$site_path'/etc/buildbot.config` 
is a Git-style config file that controls specific settings for Buildbot
Plugin.

[NOTE]
The contents of the `buildbot.config` is cached at startup by Plugin. 
If you modify any properties in this file, Plugin needs to be restarted 
before it will use the new values.

The file is composed of one `user` and `log` section and one or more `project` 
sections. Each project section provides configuration settings for one or more 
trigger strategie, reviewerGroupName and branch.

user.mail
:	EMail of (optionally non interactive account) owner of buildbot. 
        Buildbot verification are published by this user. Mandatory.

log.directory
:       Directory where log files are put. Those are the log of verification.
        Currently these log are published through plugins own LogServlet. 
        In future jenkins integration would be provided.

In the keys below, the `NAME` portion identify a project name, and
must be unique to distinguish the different sections if more than one
project appears in the file.

project.NAME.trigger
:       Trigger Strategie for the project. 3 Strategies are supported:
*        positive_review: see below
*        patchset_created: Build is triggered undonditionally when patch set is created
*        manually: Build can be only triggered by `schedule` ssh command.

positive_review explanation: Build is triggered when the follow conditions are met:

* it is not merged
* it has no review < 0,
* it has no verify < 0,
* it has no verify > 0,
* it has at least 1 review > 0 from a user that belong to the `Reviewer` Group (see `reviewerGroupName` below)
* no build job is pending for this patch set

project.NAME.reviewerGroupName
:       Group name for positive_review trigger strategie. For this strategie this 
        option is mandatory. For all other strategies this option is ignored.

project.NAME.branch
:       Branch name to restrict the build triggering to. Optionally.

Sample `buildbot.config`:

```
[user]
  mail = buildbot@example.com

[project "foo"]
  branch = master
  trigger = manually

[project "bar"]
  trigger = positive_review
  reviewerGroupName = Reviewer
  branch = master

[project "baz"]
  branch = master
  trigger = patchset_created

[log]
  directory = /var/data/gerrit_buildlog_directory
```
