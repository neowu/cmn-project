## Change log

### 1.9.10 (11/29/2019 - 11/29/2019)
* java: update to JDK13
* aws: update to version 1.11.

### 1.9.9.1 (7/23/2019 - 7/23/2019)
* bug: fix subnets parameter typo

### 1.9.9 (7/16/2019 - 7/17/2019)
* bug: fix NPE on DeleteELBTask workflow

### 1.9.8 (2/21/2019 - 4/26/2019)
* aws: update to version 1.11.540
* gradle: update to version 5.4
* kafka: update role to version 2.2.0
* es: update role to version 7.0.0
* kibana: update role to version 7.0.0
* log-processor: update role to version 6.12.4

### 1.9.7 (10/10/2018 - 10/12/2018) ¡Requires JDK11!
* log-processor: update role to version 6.6.4
* kibana: update role to version 6.3.2
* es: update role to version 6.3.2
* aws: update to version 1.11.427
* jdk: update to JDK11

### 1.9.6 (9/6/2018 - 9/6/2018)
* node: fix node repo ansible role
* aws: update to version 1.11.402

### 1.9.5 (9/3/2018 - 9/3/2018)
* node: update to version 10.9.0
* aws: update to version 1.11.401
* gradle: update to version 4.10

### 1.9.4 (6/26/2018 - 8/14/2018)
* app: add app-legacy role using jdk8 - temporary role -
* aws: update to version 1.11.374
* kafka: fix download URL
* ansible: fix install command

### 1.9.3 (6/25/2018 - 6/26/2018)
* kafka: update to version 1.1.0
* kibana: update to version 6.3.0
* es: update to version 6.3.0
* app: add temporary workaround to fix jdk10 / ubuntu18 trustAnchors issue

### 1.9.2 (5/18/2018 - 6/24/2018)
* ansible: update app role to ubuntu 18
* jdk: upgrade to jdk10
* gradle: update to gradle 4.8
* aws: update to latest SDK version

### 1.9.1 (5/31/2018 - 5/31/2018)
* aws: update to latest SDK version
* gradle: update quality lib

### 1.9.0 (3/2/2018 - 4/18/2018)
* jdk: update to jdk 9 compatible
* elasticsearch: update/fix elasticsearch 6.x role
* kubernetes: update/fix kubernetes 1.9.6 role

### 1.8.9 (2/28/2018 - 2/28/2018)
* tg: fix sync issue (always tried to update target group)

### 1.8.8 (1/25/2018 - 2/19/2018)
* ec2: add support for m5 instance type
* elb: add support for application ELB

### 1.8.7 (12/6/2017 - 12/6/2017)
* node: update to 9.2.0

### 1.8.6 (3/3/2017 - 11/28/2017)
* kafka: update to 1.0.0
* docker: update to use docker-ce
* kibana: update to 6.0.0

### 1.8.5 (12/8/2016 - 2/27/2017)
* aws: fix AWS latest SDK deprecated old IpRange and switch to Ipv4Ranges
* kafka: update kafka to 0.10.1.1
* redis: support redis max memory policy for cache

### 1.8.4 (10/15/2016 - 12/01/2016)
* provision: removed ubuntu 14.04 support from ansible provisioner, now build-in roles and provision/bake only works with ubuntu 1604
* ec2: create instance into multiple AZ/Subnet if applicable
* exec: support multiple cmd

### 1.8.3 (10/28/2016 - 10/15/2016)
* es: update es/kibana to 5.0.0
* protocol: add all-tcp/all-udp for kubernetes cluster support

### 1.8.2 (10/20/2016 - 10/27/2016)
* kafka: update to 0.10.1.0
* nginx: removed PPA support, use official ubuntu repo

### 1.8.1 (10/12/2016)
* mongo: disable NUMA, for ec2 m4.xlarge or larger instance
* ansible: role behavior changed with ubuntu1604, all roles are only for bake image, service will not start by default

### 1.8.0 (6/27/2016 - 10/11/2016)
* redis: update ansible playbook to support persistence rdb/aof
* docker: add docker engine role
* kibana: update kibana role to use download tar
* logroate: simplify logroate config, not upload to s3
* ubuntu: update all roles for ubuntu 16.04, removed supervisor/ec2 roles
* kafka: add kafka role
* jdk: move to openjdk-8-jre-headless with ubuntu 16.04
* tomcat: update to tomcat8

### 1.7.9 (6/8/2016 - 6/14/2016)
* rabbitmq: updated key path
* app: added app role into cmn
* kibana: added kibana role, now it has officially deb repo

### 1.7.8 (5/30)
* provision: update version for fixing uninterrupted dist-upgrade

### 1.7.7 (5/25)
* provision: run apt-get update dist-upgrade before ansible playbook
* sys: remove apt-get update/upgrade from role  (which may cause ansible problem if update ansible during playbook execution)

### 1.7.6 (4/28 - 5/17)
* ansible: jenkins supports plugins
* provision: use private ip if public dns not available, for build server provision private subnet server (by Gabo)
* deploy: retry on throttling of ASG update

### 1.7.5 (3/28/2016 - 4/28)
* mongodb: clustering support
* node: added repo
* bake: delete previous failed AMI during bake

### 1.7.4 (3/22/2016)
* ec2: support ebs type
* mongodb: support db path as variable

### 1.7.3 (3/1/2016)
* ec2: pass ebs-optimized = true for m4/c4 instance, it's not necessary since it's enabled by default, but there is bug in AWS console, it needs to pass it to display correct value

### 1.7.2 (2/3/2016 - 2/19/2016)
* es: install HQ plugin by default
* redis: set timeout to 7200s
* mongo: add logrotate conf
* cert: support Amazon Cert for ELB

### 1.7.1 (1/25/2016 - 2/3/2016)
* ansbile: update command line param according to latest version (sudo->become)
* mongo: update mongo role to 3.2.1
* redis/mongo: add tuning settings, disable thp, set max nofile, maxconn, overcommit
* supervisor: updated supervisor init script to adapt to ansible service restart (ansible "retart service" calls stop->start without delay)

### 1.7.0 (12/29/2015 - 1/4/2016)
* nat: support nat gateway
* ssh: support tunnel to ssh to private subnet

### 1.6.9 (12/18/2015 - 12/29/2015)
* iam: handle deleting instance profile but without role (not expected state, but make cmn ignore it)
* cloudwatch: removed ec2_cloudwatch_metrics, not useful to us
* ec2: delete snapshot when removing images

### 1.6.8 (12/10/2015 - 12/17/2015)
* ansible: removed varnish, don't plan to use
* docker: added docker folder for local dev
* sns: removed sns and sqs support
* tag: AWS bugs, the tag system may return old AMI id, put fix to ignore and warning if it happens
* elasticsearch: update default bulk queue_size from 50 to 500

### 1.6.7 (12/03/2015)
* nginx: update gzip type (remove text/html since it's default)

### 1.6.6 (11/23/2015)
* ec2: start instance will reattach instance to ELB, due to ELB will not refresh instance if its IP changed

### 1.6.5 (10/30/2015 - 11/04/2015)
* elasticsearch: update to 2.0
* elasticsearch: general tuning settings

### 1.6.4 (10/30/2015)
* task: validate resourceId passed in
* deploy: wait random time before deploy to avoid AWS ASG rate limit

### 1.6.3 (10/29/2015)
* elb: check cert local config if specified
* elb: AWS IAM cert deletion behavior changed, make update cert to delete ELB listener first

### 1.6.2 (10/27/2015 - 10/28/2015)
* ansible: update jenkins key url according to https://wiki.jenkins-ci.org/display/JENKINS/Installing+Jenkins+on+Ubuntu
* ec2: retry runInstance if request rate limit exceeds, (may happen on baking many instances during build)

### 1.6.1 (10/19/2015)
* lib: update aws lib to latest
* bake: remote AMI can be in non available state, load remote images state during loading, to old delete out dated available AMI during bake

### 1.6.0 (9/21/2015 - 10/14/2015)
* lib: update lib up to date
* nginx: updated gzip type

### 1.5.9 (9/8/2015)
* subnet: sort subnet by AZ, to make "instance" resource always uses first AZ, (deterministic behavior to make reserved instance easier to plan)

### 1.5.8 (8/20/2015)
* ansible: rabbitmq install rabbitmq admin script

### 1.5.7 (8/13/2015 - 8/17/2015)
* ansible: supervisor supports empty env
* ansible: logrotate keep 7 days old log if not move to S3
* ansible: add kibana role

### 1.5.6 (7/29/2015 - 8/6/2015)
* validate ELB name
* updated AWS sdk to 1.10.8
* optimized ASG loading, use one request to load all LaunchConfig
* fix ASG delete planner, to use remote resource only for sync with deletion

### 1.5.5 (7/18/2015 - 7/20/2015)
* update elasticsearch role to 1.7
* instance deploy, wait until InService

### 7/15/2015 (1.5.4)
* fix: strict linking between Image and unfinished bake instances, to clean up unfinished bake instances correctly

### 7/14/2015 (1.5.3)
* fix: bake AMI sg name should be unique across env

### 7/14/2015 (1.5.2)
* for SSH goal, make minimal AWS calls

### 7/6/2015 (1.5.1)
* remove InstanceType enum, use String instead, so support all types
* IAM path can not contains '-' (AWS doc is wrong), revert path transform logic back 

### 6/26/2015 - 7/5/2015 (1.5.0)
* updated AWS sdk version 
* update nginx role to support custom conf
* SQS supports China region
* update EC2.availabilityZones() to return available ones
* create IAM instanceProfile and cert with original env name as path

### 6/25/2015 (1.4.9)
* provision will use ami() package-dir/playbook by default

### 6/22/2015 - 6/24/2015 (1.4.8)
* support cn-north-1 region, which doesn't have M4/C4 instances, bake AMI will use M3.Large
* fixed unnecessary space in local cert will cause re-update cert.  

### 6/17/2015 (1.4.7)
* update rabbitmq/elasticsearch roles, to add log rotate support and other config
* recreate instance if new instance profile added or deleted
* update elasticsearch to 1.6.0
* update default nginx proxy setting to forward http_port, to make core-ng can construct requestURL

### 6/12/2015 - 6/16/2015 (1.4.6)
* support new M4 instances
The trends of AWS is to use VPC/HVM instance, and no more ephemeral volumes, and enable EBS-optimized by default
in future we should only use t2/m4/c4 instances, and use HVM ubuntu linux, this will simplify cmn
* remove EBS-optimized configuration
* remove mount ephemeral disk as SWAP ec2 scripts,
* refactory log folder and logrotate script to make sure shutdown hook gz log properly
* instance deploy will wait until ELB attach done

### 6/9/2015-6/10/2015 (1.4.5)
* make VPC required
* env config added, "bake-subnet-id:" for account doesn't have default VPC
* updated tomcat/supervisor/nginx role, not start on bake

### 6/6/2015 (1.4.4)
* update rabbitmq role

### 5/21/2015 (start 1.4.4)
* add supervisor support for core-ng application

### 5/21/2015 (1.4.3)
* fixed ASG deployment issue, with manually updated ASG size, the deployment may not use right maxSize

### 5/16/2015 (start 1.4.3)
* updated mongodb role to latest 3.0 and config

### 5/13/2015 (1.4.2)
* add port range for SG, for passive FTP
```
#!yml
- security-group[dev]:
    ingress:
      - {cidr: 0.0.0.0/0, protocol: [ssh, http, 30000-40000]}
```

* support subscribe queue to topic
```
#!yml
- sqs[queue-1]:

- sqs[queue-2]:

- sns[topic]:
    sqs-subscription: [queue-1, queue-2]
```

### 5/7/2015 (start 1.4.2)
* explicitly shutdown old instance at end of ASG deploy, as ASG has issue may not choose oldest instance sometimes (probably because of multi-az) 
* bake AMI can return "failed" state, break if it's failed.
* updated elasticsearch role to allow groovy script

### 5/6/2015 (1.4.1)
* added build and jenkins role to provide simple support for build server

### 4/30/2015 (continue 1.4.1)
* simplified attach to elb task, now "deploy instance" will attach to ELB in time.

### 4/29/2015 (start 1.4.1)
* structure refactory, simplify task design, prepare for further refactory

### 4/28/2015 (1.4.0)
* add java monitor to collect heap usage and thread count

### 4/28/2015 (1.3.9)
* !!! reorganized ec2/logrotate ansible roles, it's better to rebake baseAMI
* validate allowed/required param from command line input

### 4/27/2015 (continue 1.3.9)
* update SSH runner to keep alive with SSH session
* add memUsage cloudwatch metrics in EC2

### 4/24/2015 (start 1.3.9, wait for testing later)
* deploy ASG, check ELB state of new instances if applicable.

### 4/23/2015
* removed memcached and activemq roles, don't use them anymore, and you can put into your env/ansible folder for custom roles.

### 4/22/2015 (1.3.8)
* update tomcat default roles to support use "cmn provision" to release for non-ASG instances
* update ASG always use OldestInstance as termination policy, due to OldestLaunchConfig may not work properly when config was deleted during deployment

### 4/21/2015 (1.3.7)
* enable ELB multi-zone load balancing for multi-az

### 4/20/2015 (1.3.6)
* refactored AWS client to use plain design for simplicity and flexibility
* removed disabled resource support, always check all since most of them will be used
* support updating iam profile policy

### 4/18/2015
* update health check settings for high load scenario

### 4/17/2015 (1.3.5)
* only use HTTP connector of tomcat (according to perf test on AWS)
* nginx set x-forwarded-proto smartly, enable keep alive between nginx and tomcat
* ELB always forward to http port

### 4/15/2015 (1.3.3/1.3.4)
* bake: use --resume-bake=true to auto pick previous instance,
* bake: bake will clean up previous failed instance/sg/key
* deployment: make ASG deployment more robust

### 4/13/2015 (1.3.2)
* support "scheme: internal" for ELB in public subnet used for internal
* fix delete VPC should depends on delete SG

### 3/23/2015 (1.3.0)
* support deploy goal
* removed instance bootstrap script support, (not needed anymore)

### 3/16/2015
* make jdk 8 as default
* support mac os
* fix: auto assign public ip to AS group in public subnet
