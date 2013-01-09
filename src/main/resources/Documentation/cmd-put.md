put - Report task result
========================

NAME
----
put - Acknowledge executed task and report the result.

SYNOPSIS
--------
>     ssh -p <port> <host> buildbot put
>      --ticket <ID> | -t <ID>
>      --log <url> | -l <url>
>      --status <SUCCESS|FAILED|CANCELED> | [-s]
>      --id <TB-ID> | -i <TB-ID>

DESCRIPTION
-----------
Once the task is executed, buildbot returns it status and log with
`put` command. When all platform specific tasks for Gerrit change are
executed, Gerrit change is verified.

If tasks for all platforms have status succees then verifiy +1
is provided, otherwise verifiy -1.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group,
or have been granted [the 'View Queue' global capability][1].

[1]: ../../../Documentation/access-control.html#capability_viewQueue

SEE ALSO
--------

* [get](cmd-get.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)