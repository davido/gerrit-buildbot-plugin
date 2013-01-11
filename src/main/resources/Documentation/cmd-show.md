@PLUGIN@ show
=============

NAME
----
@PLUGIN@ show - Display the buildbot work queue.

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ show
  --project <NAME> | -p <NAME>
  [--type {change | job | all}]
```

DESCRIPTION
-----------
Presents a table of the pending activity the @PLUGIN@ plugin
is currently performing. @PLUGIN@ plugin maintains queue, that it
uses to queue build task for different platforms. Buildbots poll the
tasks for execution and report the result back.

ACCESS
------
Caller must be a member of the privileged ['Administrators'][1] group,
or have been granted the ['View Queue' global capability][2].

[1]: ../../../Documentation/access-control.html#administrators
[2]: ../../../Documentation/access-control.html#capability_viewQueue

SCRIPTING
---------
This command is intended to be used in scripts.

OPTIONS
-------

`--project`
:	Name of the project to show the tasks for. The project must be
	configured in `$gerrit_site/etc/buildbot.config` file.

`--type`
:	Type og the items to show: `job` and `task` types are supported.
	Default is to show all items.

This queue contains 4 tasks: one job and 3 tasks scheduled for build
--------

```
    $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ show --project foo
      ------------------------------------------------------------------------------
      Task-Id           Start/End    Type/State  Ref
      927700101         20:35:41     Change      refs/changes/33/33/10
      927700101_WINDOWS -            Job: INIT   refs/changes/33/33/10
      927700101_LINUX   -            Job: INIT   refs/changes/33/33/10
      927700101_MAC     -            Job: INIT   refs/changes/33/33/10
      ------------------------------------------------------------------------------
```

SEE ALSO
--------

* [config](config-buildbot.html)
* [get](cmd-get.html)
* [put](cmd-put.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)