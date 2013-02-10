@PLUGIN@ schedule
=================

NAME
----
@PLUGIN@ schedule - Manually trigger a build for specified patch sets

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ schedule
  [--project <PROJECT> | -p <PROJECT>]
  [--ticket <SHA-1>_<PLATFORM> | -t <SHA-1>_<PLATFORM>]
  [--drop | -d ]
  [--force | -f] 
  [--all | -a]
  [{COMMIT | CHANGEID,PATCHSET}...]
```

DESCRIPTION
-----------
Schedules build of the specified patch sets or drop item(s) from the queue.

When `--drop` is provided, then an item is dropped from the queue.
When `--all` is provided then the whole queue is cleared.

Patch set should be specified as complete or abbreviated commit SHA-1.

For current backward compatibility with user tools patch sets may
also be specified in the legacy 'CHANGEID,PATCHSET' format, such as
'8242,2'.  Support for this legacy format is planned to be remove
in a future edition of @PLUGIN@ plugin.  Use of commit SHA-1s
is strongly encouraged.

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
:	Name of the project the intended changes are contained
	within. This option must be supplied before the commit
	SHA-1 in order to take effect.

`--ticket`
:	Ticket to drop items for. When `--ticket` in the form <SHA-1> is 
	specified then the build job for this SHA-1 is dropped from the queue.
	When `--ticket` in the form <SHA-1>_<platform> is specified, then the
	corresponding task is dropped from the queue.

`--drop`
:	Drop an item from the queue. Only items that are in pinding state are dropped.
        To drop items in running state `--force` must be provided.

`--all`
:	Clear the whole queue. If the `--project`option is specified then
	only jobs and tasks are dropped for this project. If `--force`
	option is specified then running tasks are also dropped.

`--force`
:	Option which allows @PLUGIN@ to clear items from the queue, even when
	some tasks have running state. This option must be supplied together 
	wird `--drop` option to have effect. 

EXAMPLES
--------
Trigger a build for specified patch set:

```
  $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ schedule --project foo c0ff33123
```

Drop the job from the queue:

```
  $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ schedule --drop --ticket c0ff33123
```

SEE ALSO
--------

* [get](cmd-get.html)
* [put](cmd-put.html)
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