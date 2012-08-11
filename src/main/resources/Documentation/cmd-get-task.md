get-task - Get a task from platform specific queue
===================

NAME
----
get-task - Get a task from platform specific queue.

SYNOPSIS
--------
>     ssh -p <port> <host> buildbot get-task
>      [--project <NAME> | -p <NAME>]
>      --platform <NAME> | -a <NAME>]

DESCRIPTION
-----------
To get a task for building builbot connect to buildbot plugin and
poll a task from a platform specific. Once the task i spolled, it
is removed from the blocking queue and a new thread is started.
This thread wait until the task is reported as success, fail or cancel
with report ssh command. If result is not reported and timeoutdefined
in buildbot.config is expied the task status is timedout.

Once the task is executed, buildbot returns it status and log with
report command.

Note: Platform can be one from {windows | linux | mac}.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group,
or have been granted [the 'View Queue' global capability][1].

[1]: ../../../Documentation/access-control.html#capability_viewQueue

SEE ALSO
--------

* [report](cmd-report.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)