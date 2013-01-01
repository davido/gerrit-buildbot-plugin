Buildbot Plugin - Configuration
===============================

Config data is defined in plugin specific configuration file
buildbot.config
--------------

user section
--------------

project section
---------------
project.name = foo

File `buildbot.config`
------------------------

The mandatory file `'$site_path'/data/<plugin-name>/buildbot.config` 
is a Git-style config file that controls specific settings for Buildbot
Plugin.

[NOTE]
The contents of the `buildbot.config` is cached at startup by Plugin. 
If you modify any properties in this file, Plugin needs to be restarted 
before it will use the new values.

Sample `buildbot.config`:
----
[user]
  mail = buildbot@example.com

[project]
  name = foo
  trigger = default|norbert
  reviewerGroupName = Reviewer

[log]
  directory = /var/data/gerrit2_buildlog_directory
----
