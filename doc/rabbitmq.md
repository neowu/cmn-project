## config
currently we manage RabbitMQ cluster manually, no need for dynamic scale out

rabbitmq conf is tied to ip, all config set during bake will be lost when creating new server by using the AMI.
after creating the server, it requires to run "cmn provision --id=queue" to setup admin/rabbitmq users and other config

node name can be configured via via https://www.rabbitmq.com/man/rabbitmq-env.conf.5.man.html,
currently it's not necessary to maintain thru cmn.

## cluster
* port 4369, 25672 needs to open between cluster nodes, typical sg setup is like following
```
- security-group[queue]:
    ingress:
      - {cidr: 0.0.0.0/0, protocol: [ssh, rabbitmq, 15672]}
      - {security-group: queue, protocol: [4369, 25672]}
```

* erlang cookie need to be specified for all nodes
```
rabbitmq_erlang_cookie: "xxx"
```

* on new server, following commands is to join cluster (as root)
```
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@master
rabbitmqctl start_app
```

use HA policy to mirror all queues and exchanges

```
{
    "rabbit_version": "3.5.4",
    "vhosts": [
        {
            "name": "/"
        }
    ],
    "policies": [
        {
            "vhost": "/",
            "name": "ha-all",
            "pattern": ".*",
            "apply-to": "all",
            "definition": {
                "ha-mode": "exactly",
                "ha-params": 2,
                "ha-sync-mode": "automatic"
            },
            "priority": 0
        }
    ],
    "queues": [
        {
            "name": "test",
            "vhost": "/",
            "durable": true,
            "auto_delete": false,
            "arguments": {}
        }
    ],
    "exchanges": [],
    "bindings": []
}
```