@PLUGIN@ put
============

NAME
----
@PLUGIN@ put - Acknowledge executed task and report the result.

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ put
  --ticket <ID> | -t <ID>
  --status <success|failed|canceled> | -s <success|failed|canceled>
  --id <BUILDBOT> | -i <BUILDBOT>
  [--log <url> | -]
```

DESCRIPTION
-----------
Once the task is executed, buildbot returns it status and log with
`put` command. When all platform specific tasks for Gerrit change are
executed, Gerrit change is verified.

When tasks for all platforms have status success then verify +1
is provided, otherwise verify -1.

If buildbot returns status failed for one task, then all other tasks
for that job, that are currently pending 
(waiting to be picked up from the queue for build), are discarded.

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

`--ticket`
:	Ticket to report result for.

`--status`
:	Outcome of the build. Possible values are: `success`, `failed` or
	`canceled`. If the task is discarded then the status `discarded`.
	If the task is dropped (see `schedule` command), then the status is
	`dropped`.

`--id`
:	Buildbot id. To get a task buildbot identifies itself against @PLUGIN@
	plugin with `--id` option passed to `get` ssh command. Buildbot must
	pass the same id to report the result with `put` ssh command, otherwise
	the result is ignored.

`--log`
:	Url of the log file. If `-` the gzipped log is read from stdin, 
	rather than from the url. Log option is mandatory for the `success`
	and `failed` status.

EXAMPLES
--------
Report result `success` for specified ticket and pass gzipped log on stdin:

```
  $ cat result.log.gz | ssh -p @SSH_PORT@ gerrit @PLUGIN@ put --ticket c0ff33123_LINUX --status success --id 1 --log -
```

Report result `canceled` for specified ticket:

```
  $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ put --ticket c0ff33123_LINUX --status canceled --id 1
```

SEE ALSO
--------

* [get](cmd-get.html)
* [schedule](cmd-schedule.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)