# backup and restore
```
sudo /usr/share/elasticsearch/bin/plugin install cloud-aws

curl -XPUT localhost:9200/_snapshot/s3 -d '
{
  "type": "s3",
  "settings": {
    "bucket": "neo-test-es-backup",
    "region": "us-east-1",
    "base_path": ""
  }
}'

curl -XPOST localhost:9200/_snapshot/s3/_verify

curl -XPUT localhost:9200/_snapshot/s3/snapshot_1?wait_for_completion=true

curl -XPOST localhost:9200/_snapshot/s3/snapshot_1/_restore
```
