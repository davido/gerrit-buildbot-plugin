show - Display the buildbot work queue
=======================================

NAME
----
show - Display the buildbot work queue.

SYNOPSIS
--------
>     ssh -p <port> <host> buildbot show
>      [--project <NAME> | -p <NAME>]
>      [--type {change | job | all}]
>      [--verbose | -v]

DESCRIPTION
-----------
Presents a table of the pending activity the Builbot Plugin
is currently performing, or will perform in the near future.
Gerrit Buildbot Plugin contains an internal scheduler, that it
uses to queue Buildbot task for different platforms. Tinderboxes
schedule the tasks for execution and reports the result back.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group,
or have been granted [the 'View Queue' global capability][1].

[1]: ../../../Documentation/access-control.html#capability_viewQueue

The following queue contains 4 tasks: one change and 3 jobs scheduled to execution
--------

```
    $ ssh -p 29418 review.example.com buildbot show --project foo
      ------------------------------------------------------------------------------
      Task-Id          Start/End    Type/State  Ref
      92770010         20:35:41     Change      refs/changes/33/33/10
      92770010_WINDOWS -            Job: INIT   refs/changes/33/33/10
      92770010_LINUX   -            Job: INIT   refs/changes/33/33/10
      92770010_MAC     -            Job: INIT   refs/changes/33/33/10
      ------------------------------------------------------------------------------
```

SEE ALSO
--------

* [get](cmd-get.html)
* [put](cmd-put.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)