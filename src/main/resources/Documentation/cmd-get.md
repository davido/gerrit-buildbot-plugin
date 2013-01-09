get - Get a task from platform specific queue
=============================================

NAME
----
get - Get a task from platform specific queue.

SYNOPSIS
--------
>     ssh -p <port> <host> buildbot get
>      --format TEXT | BASH | -f TEXT | BASH
>      --project <NAME> | -p <NAME>
>      --platform <NAME> | -a <NAME>
>      --id <TB-ID> | -i <TB-ID>

DESCRIPTION
-----------
To get a task for building builbot connect to buildbot plugin and
poll a task from a platform specific. Once the task i spolled, it
is removed from the blocking queue and a new thread is started.
This thread wait until the task is reported as success, failed or 
canceled with `put` ssh command.

Note: Platform can be one from {windows | linux | mac}.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group,
or have been granted [the 'View Queue' global capability][1].

[1]: ../../../Documentation/access-control.html#capability_viewQueue

SEE ALSO
--------

* [put](cmd-put.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)