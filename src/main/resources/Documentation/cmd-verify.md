@PLUGIN@ verify
===============

NAME
----
@PLUGIN@ verify - Manually override verification status

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ gerrit @PLUGIN@ verify
  [--project <PROJECT> | -p <PROJECT>]
  [--verified <N>]
  [{COMMIT | CHANGEID,PATCHSET}]
```

DESCRIPTION
-----------
 Manually set verification status.

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

`--verified`
:       Set the approval category to the value 'N'. The exact
        option names supported and the range of values permitted
        differs per site, check the output of `review` --help.

EXAMPLES
--------
Override verification status for specified patch set:

```
  $ ssh -p @SSH_PORT@ gerrit @PLUGIN@ verify --project foo --verified 0 c0ff33123
```

SEE ALSO
--------

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