#!/usr/bin/env python
import urllib2
import json

import boto.utils
import boto.ec2.cloudwatch


def heap_usage():
    request = urllib2.Request("http://localhost:8080/monitor/memory", headers={"Accept": "application/json"})
    opener = urllib2.build_opener()
    response = opener.open(request)
    usage = json.loads(response.read())
    return float(usage['heap_used']) / usage['heap_max'] * 100


def thread_count():
    request = urllib2.Request("http://localhost:8080/monitor/thread", headers={"Accept": "application/json"})
    opener = urllib2.build_opener()
    response = opener.open(request)
    info = json.loads(response.read())
    return info['thread_count']


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

    heap = heap_usage()
    thread = thread_count()

    metric_keys = ['JavaHeapUsage', 'JavaThreadCount']
    metric_values = [heap, thread]
    metric_units = ['Percent', 'Count']

    dimensions = {'instanceId': instance_id, 'env': user_data['env'], 'id': user_data['id'], 'name': user_data['name']}

    cloudwatch = boto.ec2.cloudwatch.connect_to_region(region)
    cloudwatch.put_metric_data(user_data['env'], metric_keys, metric_values,
                               unit=metric_units,
                               dimensions=dimensions)