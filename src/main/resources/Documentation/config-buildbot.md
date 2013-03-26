@PLUGIN@ configuration
======================

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
trigger strategy, reviewerGroupName and branch.

`user.mail`
:	EMail of (optionally non interactive account) owner of buildbot. 
        Buildbot verification are published by this user. Mandatory.

`user.forgeReviewerIdentity`
:	Forge reviewer identity. With `put` command Buildbot reports verification
        status with buildbot own identity. If this option is set to `false`
        (default is `true`) then the verification status is reported under caller's
        own identity.

`log.directory`
:       Directory where log files are put. Those are the log of verification.
        Currently these log are published through plugins own LogServlet. 
        In future jenkins integration would be provided.

In the keys below, the `NAME` portion identify a project name, and
must be unique to distinguish the different sections if more than one
project appears in the file.

`project.NAME.trigger`
:	Trigger Strategy for the project. 3 Strategies are supported:
* `patchset_created`: build job is triggered unconditionally when patch set is created
* `manually`: build job can be only triggered by `schedule` ssh command.
* `positive_review`: build is triggered when the follow conditions are met, see below.

`positive_review`:
:	build for a specific patch set is triggered if and only if:
* it is not merged
* it has no review < 0,
* it has no verify < 0,
* it has no verify > 0,
* it has at least 1 review > 0 from a user that belong to the `Reviewer` Group (see `reviewerGroupName`)
* no build job is pending for this patch set

`project.NAME.reviewerGroupName`
:       Group name for `positive_review` trigger strategy. For this strategy this 
        option is mandatory. For all other strategies this option is ignored.

`project.NAME.branch`
:       Branch name to restrict the build triggering to. Optionally.

Sample `buildbot.config`:

```
[user]
  mail = buildbot@example.com

[log]
  directory = /var/data/gerrit/logs

[project "foo"]
  branch = master
  trigger = manually

[project "bar"]
  branch = master
  trigger = positive_review
  reviewerGroupName = Reviewer

[project "baz"]
  branch = master
  trigger = patchset_created
```

SEE ALSO
--------

* [get](cmd-get.html)
* [put](cmd-put.html)
* [schedule](cmd-schedule.html)
* [show](cmd-show.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)
