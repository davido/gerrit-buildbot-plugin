@PLUGIN@ changelog
==================

Version 2.1: 2014-02-22
============

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

Version 2.0: 2013-12-12
===========

Final version built against Gerrit 2.8 GA.

Version 2.0rc5: 2013-07-15
==============

Add "Buildbot" menu item to Gerrit top menu. if user is logged in and a
member of `BuildbotUserGroup` or `BuildbotAdminGroup` then the link to
Buildbot Queue is added. Otherwise only Documentation link is provided.

Version 2.0rc4: 2013-07-10
==============

Implement "Schedule"-UiCommand and place it on the current patch set action panel.

Version 2.0rc3: 2013-07-06
==============

Add Queue Servlet to show buildbot's queue content. Page is rendered with Google closure template framework (soy template).

Version 2.0rc2: 2013-06-30
===========

Add new Gerrit group `buildbotUserGroup` for buildbot users and grant the members of this group the right to call `schedule` and `show` SSH commands. This group can be overriden per project base.

get
:

* deprecated and replace `--platform` option with `--os` option

merge buildbot-2.5 branch to master

Version 2.0rc1: 2013-04-20
===========

merge buildbot-2.5 branch to master

Version 1.15: 2013-02-13
===========

ssh commands
------------

get
:

* don't forge reviewer identity for start build notification.

put
:

* don't forge reviewer identity for simple votes.
  Through the reporting under its own identity we buy us a fancy feature:
  each TB shows up in the reviewer list as an extra row on the change page.
* vote with score -1/+1 for single job
* vote +2 for combined report
* no combined vote in case of failure. This is due to not to block the patch,
  so the dev can still manually override with +2 and submit the patch.

configuration
-------------

modify the database
:

* insert into approval_category_values values('Verified', 'VRIF', 2);
* insert into approval_category_values values('Fails', 'VRIF', -2);

adjust ACL in gerrit
:

* in gerrit 'All-Project' page add buildbot ACL +2 on 'VRIF' category (note it already has +1)
* in gerrit 'All-Project' page add commiters ACL +2 on 'VRIF' category (note it already has +1)

Version 1.14: 2013-02-03
===========

core
-----------

* Adjust platform names to Windows, Linux and MacOSX.

Version 1.13: 2013-01-30
===========

implement handling for stale patch sets
---------------------------------------

* If a new patch for a change is 'submitted' while the verification tinbuild are pending then
* we let already running tindebox finish, and still report the result
* in the 'review' comment as usual, but leave the verify flags untouched
* any platform that is not started yet is 'discarded' for that patch.

Version 1.12: 2013-01-29
===========

configuration
-------------

* add `user.buildbotAdminGroupName` mandatory configuration option.

ssh commands
------------

* get `--id` is optionally. Default is user name. Only member of
  `buildbotAdminGroupName` can provide `--id` option.
* put `--id` is optionally. Default is user name. Only member of
  `buildbotAdminGroupName` can provide `--id` option.

Version 1.11: 2013-01-28
===========

ssh commands
------------

show
:

* added --dump option to dump the content of platform specific queues

test-log-channel
:

* New ssh command to test that the connection to extern log file channel was estabished successfully.

	
Version 1.10: 2013-01-27
===========

configuration
-------------

* support extern log file publication (jenkins)

Example buildbot.config
-------

```
[...]

[log]
  mode = extern
  host = ci.libreoffice.org
  url = https://ci.libreoffice.org
  job = buildbot

[...]
```

core
----

* bug fixed: race condition reporting task with status `cancelled` leads to discarding the whole job instead of replacing the task with the new one in status `INIT` and preserving the job. Test case for that is added.

Version 1.9: 2013-01-26
===========

ssh commands
------------

put
:

* american spelling for `cancelled` is added (success|failed|canceled|cancelled).
* report the TB-ID that build the task

get
:

* new option `--test` added. This is just a convinience feature so that one can debug one tinderbox configuration whithout messing with the patch verification of real patch.  Note that task is not removed from the queue. Subsequent `get` call without `--test` option would peek the same task again. Moreover, TB-ID is not registered on the queue site, so no reporting back from the same TB for that task is possible. No notification is reported back to gerrit.
* report the TB-ID that polled the task for build.

core
----

* bug fixed: race condition reporting task with status `cancelled` leads to discarding the whole job instead of replacing the task with the new one in status `INIT` and preserving the job. Test case for that is added.


Version 1.8: 2013-01-25
===========

core
----

* bug fixed: When a tb reported `failed` status back -- discarding pending tasks -- the next `get` command still engaged with the purged tasks. The try to report with `put` for purged task failed with no such task error. Test case for that is added.

test
----

complete job test and discarded tasks test added.

Version 1.7: 2013-01-19
===========

Configuration:
--------------

* new (optional configuration option) `user.forgeReviewIdentity` can be set to report verification status with Buildbot its own identity.

ssh commands
------------

verify
:

* Manually override verification status

Version 1.6: 2013-01-15
===========

core
----

* if --id option in get command wasn't match the --id option in put command, the result was still accepted. Fixed.
* Multiple project support activated

Configuration: non compatible change
------------------------------------

* support multiple projects

Example buildbot.config
-------

```
[...]
[project "foo"]
  trigger = manually
  branch = master
  branch = bar
  branch = baz

[project "bar"]
  trigger = positive_review
  reviewerGroupName = Reviewer
  branch = master
  branch = baz
[...]
```

Version 1.5: 2013-01-14
===========

core
----

* Ajust google juice binding initialization: plugin is reloaded without resource leak.

ssh commands
------------

get
:

* add support for branch[es] argument

version
:

* new command to display plugin version


Configuration
-------------

* support multiple branch names

```
branch = `<NAME1>`
branch = `<NAME2>`
[...]
```

Version 1.4: 2013-01-11
===========

core
----

build task ticket `<SHA-1>_<platform>` contained self generated SHA-1.
Original SHA-1 from patch set is used instead.

ssh commands
------------

put
:

* race condition if status --canceled for the last running task was reported.

show
:

* fixed wrong time format
* new column TB-ID (Bot) added to the table

Documentation
-------------

* schedule command: fixed some formating issues
* put command: fixed formatting issues
* get command: fixed formatting issues

Version 1.3: 2013-01-10
===========

ssh commands changes
--------------------

report
:

* renamed in put
* --log option is mandatory now for status failed or succeed.
* --succeed option dropped
* --failed option dropped
* new option --status introduced with follow values: failed|succeed|canceled
* canceled mean that TB has not any status to report. With this the --log is not mandatory. A task with for the same platform is created and is ready to be taken from the queue
* do not report true on stdout in success case
* introduce new option --id for tinderbox to identify itself. This value is compared with the one that picked that task. If the values doesn't match, that report command is discarded.
* optimize queue logic: if a task is reported as failed back, then all other tasks that are still pending discarded (removed from the queue). The gerrit job is reported back as failed to gerrit.

get-task
:

* rename in get
* introduce new option --id for tinderbox to identify itself. tb-id is saved in the task job internally and must be the same when the outcome get reported (put) back, otherwise it would be discarded.

show-queue
:

* rename in show
* change date format: add day, remove milliseconds

Configuration
-------------

* introduce new optional restriction in trigger strategie:

```
branch = `<NAME>`
```

If set, only patch sets for that branch are considered for building. That affects all kind of strategies.

Documentation
----------------

* some clean up
* ajust ssh command documentation


Version 1.2: 2013-01-07
===========
Change the location of buildbot.config file from `$gerrit_site/data/<plugin>`
to `$gerrit_site/etc`.

Version 1.1: 2013-01-01
===========
Implement positive_review build trigger strategie.

Version 1.0: 2012-08-01
===========
Fist version.

SEE ALSO
--------

* [config](config-buildbot.html)
* [get](cmd-get.html)
* [put](cmd-put.html)
* [schedule](cmd-schedule.html)
* [show](cmd-show.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)
