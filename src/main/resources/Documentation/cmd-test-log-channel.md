@PLUGIN@ test-log-channel
=============

NAME
----
@PLUGIN@ test-log-channel - Test log channel is up and running

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ test-log-channel
```

DESCRIPTION
-----------
Test the log channel is set up and the communication with extern log 
file publisher is established.

ACCESS
------
Caller must be a member of the privileged ['Administrators'][1] group,
or have been granted the ['View Queue' global capability][2].

[1]: ../../../Documentation/access-control.html#administrators
[2]: ../../../Documentation/access-control.html#capability_viewQueue

SCRIPTING
---------
This command is intended to be used in scripts.

EXAMPLES
--------
Test log channel

```
  $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ test-log-channel
```

SEE ALSO
--------

* [config](config-buildbot.html)
* [get](cmd-get.html)
* [put](cmd-put.html)

AUTHOR
------
David Ostrovsky

RESOURCES
---------
<https://github.com/davido/gerrit-buildbot-plugin>

Buildbot
--------
Part of [Gerrit Buildbot Plugin](index.html)