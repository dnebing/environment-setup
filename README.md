# environment-setup

A liferay workspace that demonstrates how to use an upgrade process to handle environment setup.

I'm a big fan of Liferay upgrade processes. They can be used for many things, they are deployed
as part of your overall project, the upgrade process(es) will only run once in any given environment
and will track the revision and therefore will not try to run it again.

Since they have access to local and remote services, they can be used to set up an environment with
required data.
