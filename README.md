## CMN
cmn is a AWS resource manager. this tool is designed to support our own project, not to be a generic aws tool.

[![Build Status](https://travis-ci.com/neowu/cmn-project.svg?branch=master)](https://travis-ci.com/neowu/cmn-project)
[![Code Coverage](https://codecov.io/gh/neowu/cmn-project/branch/master/graph/badge.svg)](https://codecov.io/gh/neowu/cmn-project)

## TODO:
* detect ELB to internal change, AS group policy changes
* make Task not tied to resource, deleteTask should pass remoteResource only, refactory Task/Workflow
* monitor, eval pcp.io? collectd (server/client) to rabbitmq to ES
* elasticsearch AWS plugin
* general linux tuning, like ulimit to sys
* redis clustering
* mongo clustering
* docker

## Known issues
* some aws resource is globally, such as IAM/S3, do not use same env name on multiple region

## Change log
please check [CHANGELOG.md](CHANGELOG.md)

## References
[doc/rabbitmq.md](doc/rabbitmq.md)

## Usage
```
cmn ${goal} --{key}={value} --{key}={value}

GOALS:
  sync        # sync local config to aws, create/delete resources accordingly (use --dry-run to check the impacts)
  del         # delete all remote resources
  desc        # describe resources
  bake        # bake ami for specified image resource
  start       # start all ec2 instances
  stop        # stop all ec2 instances
  exec        # execute command remotely via ssh
  upload      # scp local package dir to remote /opt/packages
  provision   # run playbook remotely via ssh
  ssh         # start ssh terminal for specified instance resource
  deploy      # rolling update instance or auto scaling group

CONTEXTS:
  env:              # specify env folder or use current folder if absent
  id:               # only execute goal on specific resource, useful for bake ami and execute command
  dry-run:          # print the tasks will be executed
  cmd:              # shell command to run
  script:           # exec can use command or script
  package-dir:      # upload dir to /opt/packages
  i:                # specify index of instance if multiple
  resume-bake:      # resume bake ami by using previous instance, for troubleshooting purpose

EXAMPLES:
  cmn desc
  cmn sync --dry-run=true
  cmn bake --id={imageId}
  cmn exec --id={instanceId} --id={instanceId} --cmd={command}
  cmn exec --id={instanceId} --script={scriptPath}
  cmn upload --id={instanceId} --package-dir={packageDir}
  cmn ssh --id={instanceId} --i={optionalIndex}
  cmn provision --id={instanceId} --playbook={optionalPlaybookPath} --package-dir={optionalPackageDir}
  cmn deploy --id={asGroupId}
```
