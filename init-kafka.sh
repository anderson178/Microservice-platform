#!/bin/bash

/etc/kafka/docker/run &

echo "Waiting for Kafka to be ready..."
while ! nc -z localhost 9092; do
  sleep 1
done

echo "Creating kafka topics..."
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic cancellation.request --partitions 1 --replication-factor 1
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic cancellation.response --partitions 1 --replication-factor 1

echo "All topics created. Kafka is running..."

wait