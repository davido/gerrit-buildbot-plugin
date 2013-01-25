@PLUGIN@ changelog
==================

Version 1.8: 2013-01-25
===========

core
----

bug fixed: When a tb reported `failed` status back -- discarding pending tasks -- the next `get` command still engaged with the purged tasks. The try to report with `put` for purged task failed with no such task error.

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
