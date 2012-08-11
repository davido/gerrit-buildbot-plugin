show-queue - Display the buildbot work queue
===================

NAME
----
show-queue - Display the buildbot work queue.

SYNOPSIS
--------
>     ssh -p <port> <host> buildbot show-queue
>      [--project <NAME> | -p <NAME>]
>      [--type {change | job | all}]
>      [--verbose | -v]

DESCRIPTION
-----------
Presents a table of the pending activity the Builbot Plugin
is currently performing, or will perform in the near future.
Gerrit Buildbot Plugin contains an internal scheduler, that it
uses to queue Buildbot task for different platforms. Buildbots
schedule the tasks for execution and reports the result back.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group,
or have been granted [the 'View Queue' global capability][1].

[1]: ../../../Documentation/access-control.html#capability_viewQueue


OPTIONS
-------

--project
> project

--type
> change or job (default is all)

-h
> Display usage information.

ACCESS
------
Any user who has configured an SSH key.

SCRIPTING
---------
This command is intended to be used in scripts.

EXAMPLES
--------

The following queue contains 4 tasks: one change and 3 jobs scheduled to execution

>
>    $ ssh -p 29418 review.example.com buildbot show-queue --project=test
>      ------------------------------------------------------------------------------
>      Task-Id          Start/End    Type/State  Ref
>      92770010         20:35:41.905 Change      refs/changes/33/33/10
>      92770010_WINDOWS -            Job: INIT   refs/changes/33/33/10
>      92770010_LINUX   -            Job: INIT   refs/changes/33/33/10
>      92770010_MAC     -            Job: INIT   refs/changes/33/33/10
>      ------------------------------------------------------------------------------
>

SEE ALSO
--------

* [get-task](cmd-get-task.html)
* [report](cmd-report.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)