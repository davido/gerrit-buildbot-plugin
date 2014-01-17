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

`user.buildbotAdminGroupName`
:	Group name for buildbot admin group. Only member of this group can provide
        optionally --id parameter for `put` and `get` commands. Mandatory.

`user.buildbotUserGroupName`
:	Group name for buildbot user group. The member of this group can call
        `schedule` and `show` commands. Mandatory.

`user.forgeReviewerIdentity`
:	Forge reviewer identity. With `put` command Buildbot reports verification
        status with buildbot own identity. If this option is set to `false`
        (default is `true`) then the verification status is reported under caller's
        own identity.

`log.mode`
:       `intern` or `extern` log file publication mode. `Intern`: local servlet is
        used. `extern`: only jenkins `external-monitor-job` supported.

`log.directory`
:       Directory where log files are put (used for `intern` mode only).

`log.host`
:       Host name from Open SSH config file ~/.ssh/config

`log.url`
:       Url to repot back to gerrit.

`log.job`
:       Job name. Url reported back to gerrit is of the form `${url}/${job}/build-number`


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

`project.NAME.buildbotUserGroupName`
:       Project specific group name for buildbot user group. If missing, the `user.buildbotUserGroupName` used. Optionaly.

`project.NAME.branch`
:       Branch name to restrict the build triggering to. Optionally.

`project.NAME.buildbotAdminGroupName`
:       Overwrite a global `buildbotAdminGroupName` on project base.

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

Activity log configuration
--------------------------

Allow configuration of custom logging categories to produce activity log.
To configure activity log custom log4j.configuration file must be provided.

It should be done `$gerit_site/etc/gerrit.config` file, under container section:

```
[container]
        javaOptions = -Dlog4j.configuration=file:///home/gerrit/site/etc/log4j.properties

```

Example of log4j configuration file log4j.properties:

```
log4j.rootCategory=ERROR, stderr, file
log4j.appender.stderr=org.apache.log4j.ConsoleAppender
log4j.appender.stderr.target=System.err
log4j.appender.stderr.layout=org.apache.log4j.PatternLayout
log4j.appender.stderr.layout.ConversionPattern=[%d] %-5p %c %x: %m%n

log4j.appender.file=org.apache.log4j.RollingFileAppender
log4j.appender.file.File=/home/davido/projects/test_site_master/logs/gerrit.log
log4j.appender.file.MaxFileSize=10MB
log4j.appender.file.MaxBackupIndex=10
log4j.appender.file.layout=org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern=[%d] %-5p %c %x: %m%n

#tb activity
log4j.appender.tb_activity = org.apache.log4j.DailyRollingFileAppender
log4j.appender.tb_activity.DatePattern = '.'yyyy-MM-dd
log4j.appender.tb_activity.File = /home/davido/projects/test_site_master/logs/tb_activity.log
log4j.appender.tb_activity.layout = org.apache.log4j.EnhancedPatternLayout
log4j.appender.tb_activity.layout.ConversionPattern = [%d{ISO8601}{UTC}]%5p%6.6r[%t] (%F:%L) - %m%n
log4j.appender.tb_activity.Threshold=INFO

#adm activity
log4j.appender.adm_activity = org.apache.log4j.DailyRollingFileAppender
log4j.appender.adm_activity.DatePattern = '.'yyyy-MM-dd
log4j.appender.adm_activity.File = /home/davido/projects/test_site_master/logs/adm_activity.log
log4j.appender.adm_activity.layout = org.apache.log4j.EnhancedPatternLayout
log4j.appender.adm_activity.layout.ConversionPattern = [%d{ISO8601}{UTC}]%5p%6.6r[%t] (%F:%L) - %m%n
log4j.appender.adm_activity.Threshold=INFO

# alternative date format:
#{ISO8601}{GMT+1}

# GET/PUT command
log4j.logger.buildbot.tb_activity_log=INFO,tb_activity
# SCHEDULE command
log4j.logger.buildbot.adm_activity_log=INFO,adm_activity

``` 

SEE ALSO
--------

* [get](cmd-get.html)
* [put](cmd-put.html)
* [schedule](cmd-schedule.html)
* [show](cmd-show.html)

AUTHOR
------
David Ostrovsky

RESOURCES
---------
<https://github.com/davido/gerrit-buildbot-plugin>

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)
