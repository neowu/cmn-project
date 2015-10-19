#!/usr/bin/env python
import re

import boto.utils
import boto.ec2.cloudwatch


def mem_usage():
    info = {}
    pattern = re.compile('([\w\(\)]+):\s*(\d+)(:?\s*(\w+))?')
    with open('/proc/meminfo') as output:
        for line in output:
            match = pattern.match(line)
            if match:
                info[match.group(1)] = float(match.group(2))

    usage = 100 - (info['MemFree'] + info['Buffers'] + info['Cached']) / info['MemTotal'] * 100
    return usage


def user_data():
    user_data = {}
    items = boto.utils.get_instance_userdata().split('&')
    for item in items:
        name, value = item.partition('=')[::2]
        user_data[name] = value
    return user_data


if __name__ == '__main__':
    user_data = user_data()
    metadata = boto.utils.get_instance_metadata()
    instance_id = metadata['instance-id']
    region = metadata['placement']['availability-zone'][0:-1]

    mem_usage = mem_usage()

    metrics = {'MemUsage': mem_usage}
    dimensions = {'instanceId': instance_id, 'env': user_data['env'], 'id': user_data['id'], 'name': user_data['name']}

    cloudwatch = boto.ec2.cloudwatch.connect_to_region(region)
    cloudwatch.put_metric_data(user_data['env'], metrics.keys(), metrics.values(),
                               unit='Percent',
                               dimensions=dimensions)