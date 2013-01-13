@PLUGIN@ version
================

NAME
----
@PLUGIN@ version - Display the @PLUGIN@ version.

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ version
```

DESCRIPTION
-----------
Show the version of @PLUGIN@.

ACCESS
------
Caller must be a member of the privileged ['Administrators'][1] group,
or have been granted the ['View Queue' global capability][2].

[1]: ../../../Documentation/access-control.html#administrators
[2]: ../../../Documentation/access-control.html#capability_viewQueue

SCRIPTING
---------
This command is intended to be used in scripts.


EXAMPLE
-------

```
    $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ version
      buildbot version 1.5
```

SEE ALSO
--------

* [get](cmd-get.html)
* [put](cmd-put.html)
* [show](cmd-show.html)

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)