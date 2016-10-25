./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group log-processor

./kafka-topics.sh --zookeeper localhost:2181 --list

./kafka-topics.sh --zookeeper localhost:2181 --describe