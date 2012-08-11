report - Report task result
====================

NAME
----
receipt-job - Acknowledge executed task and report the result.

SYNOPSIS
--------
>     ssh -p <port> <host> buildbot report
>      --ticket <ID> | -t <ID>
>      --log <url> | -l <url>
>      [--succeed] | [-s]
>      [--failed] | [-f]

DESCRIPTION
-----------
Once the task is executed, buildbot returns it status and log with
report command. When all platform specific tasks for Gerrit change are
executed, Gerrit change is vrefified with git review command.

If tasks for all platforms have status succeed then verifiy +1
is provided, otherwise verifiy -1.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group,
or have been granted [the 'View Queue' global capability][1].

[1]: ../../../Documentation/access-control.html#capability_viewQueue

SEE ALSO
--------

* [get-task](cmd-get-task.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)