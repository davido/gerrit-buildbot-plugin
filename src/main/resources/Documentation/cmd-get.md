@PLUGIN@ get
============

NAME
----
@PLUGIN@ get - Get a task from platform specific queue.

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ get
  --project <NAME> | -p <NAME>
  --platform <NAME> | -a <NAME>
  --id <BUILDBOT> | -i <BUILDBOT>
  [--format <TEXT | BASH> | -f <TEXT | BASH>]
  [--test] | [-t]
  {BRANCH...}
```

DESCRIPTION
-----------
To get a task for building builbot connect to @PLUGIN@ plugin and
poll a task from a platform specific queue. Once the task is polled, it
is removed from the queue and a new thread is started. This thread wait
until the task is reported as success, failed or canceled with `put` ssh
command. The task can also be dropped or rescheduled with `schedule` ssh
command.

The result of task assignment is returned on the stdout:

* `GERRIT_TASK_TICKET`: ticket in the form `<SHA-1>_<platform>`
* `GERRIT_TASK_BRANCH`: branch name of the gerrit patch
* `GERRIT_TASK_REF`:    gerrit reference to pull from

If no tasks for the platform are currently contained in the queue, then
the word `empty` is returned on stdout.

With `--platform` option set to `BASH` shell compatible result is returned,
and can be sourced direct by caller shell process.

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
:	Name of the project to trigger a build for. The project must be
	configured in `$gerrit_site/etc/buildbot.config` file.

`--platform`
:	Platform to get task for. Currently platforms are hard coded and must
	be one from `windows`, `linux` or `mac`. In future platform would be
	configurable and can be set on project base.

`--id`
:	Buildbot id. Optionaly. Per default TB-ID is the user name of the gerrit user.
        Only authorised users can provide this option manually.
        To get a task buildbot identifies itself against @PLUGIN@
	plugin with `--id` option passed to `get` ssh command. Buildbot must
	pass the same id to report the result with `put` ssh command, otherwise
	the result is ignored.

`--format`
:	Output format of this command. Default is `TEXT`. If this command is
	called from `shell` script then it must be usefull to source the
	outcome. Format `BASH` does just that and set the variables, each on
	new line.

`--test`
:	Peek a task for a tinderbox test. The task is not removed from the queue
        and no reporting for that task is possible.

EXAMPLES
--------
Get a task for building:

```
  $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ get --project foo --platform linux
```

SEE ALSO
--------

* [config](config-buildbot.html)
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