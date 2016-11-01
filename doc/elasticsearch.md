# backup and restore
```
sudo /usr/share/elasticsearch/bin/plugin install cloud-aws

curl -XPUT localhost:9200/_snapshot/s3 -d '
{
  "type": "s3",
  "settings": {
    "bucket": "s3-es-backup",
    "region": "us-east-1"
  }
}'

curl -XPOST localhost:9200/_snapshot/s3/_verify

curl -XPUT localhost:9200/_snapshot/s3/snapshot_1?wait_for_completion=true

curl localhost:9200/_snapshot/s3/_all

curl -XPOST localhost:9200/_snapshot/s3/snapshot_1/_restore
```

# inspection
curl 'localhost:9200/_cat/master?v'
curl 'localhost:9200/_cat/count?v'
curl 'localhost:9200/_cat/indices/*?v'
curl 'localhost:9200/_cat/nodes?v'
