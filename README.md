## CMN
cmn is a AWS resource manager. this tool is designed to support our own project, not to be a generic aws tool.

## TODO:
* rabbitmq conf is tied to ip, after bake, all conf is gone, if specify node name via https://www.rabbitmq.com/man/rabbitmq-env.conf.5.man.html
  the name will be conflict in cluster, think about how to manage rabbitmq cluster later
* general linux tuning, like ulimit to sys 

## Known issues
* some aws resource is globally, such as IAM/S3, do not use same env name on multiple region

## Change log
please check [CHANGELOG.md](CHANGELOG.md)