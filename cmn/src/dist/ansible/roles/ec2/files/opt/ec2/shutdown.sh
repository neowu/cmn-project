#!/bin/bash
/usr/sbin/logrotate -f /etc/logrotate.conf
/opt/ec2/s3-upload-log.sh
/sbin/poweroff -p