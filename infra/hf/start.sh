#!/usr/bin/env bash
# Starts Redis, Kafka (KRaft), the three Spring Boot services and Caddy. If any process dies,
# the container exits and Hugging Face restarts it.
set -euo pipefail
mkdir -p /data/kafka/logs /data/kafka/applogs

redis-server --port 6379 --save "" --appendonly no --maxmemory 256mb --maxmemory-policy allkeys-lru &

cat > /data/kafka/server.properties <<EOF
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093
listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
advertised.listeners=PLAINTEXT://localhost:9092
controller.listener.names=CONTROLLER
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
log.dirs=/data/kafka/logs
num.partitions=1
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
auto.create.topics.enable=true
EOF
if [ ! -f /data/kafka/logs/meta.properties ]; then
  /opt/kafka/bin/kafka-storage.sh format -t "$(/opt/kafka/bin/kafka-storage.sh random-uuid)" -c /data/kafka/server.properties >/dev/null
fi
KAFKA_HEAP_OPTS="-Xmx512m -Xms256m" /opt/kafka/bin/kafka-server-start.sh /data/kafka/server.properties >/data/kafka/applogs/stdout.log 2>&1 &
for _ in $(seq 1 90); do (echo >/dev/tcp/127.0.0.1/9092) 2>/dev/null && break; sleep 1; done
echo "kafka up"

export KAFKA_BOOTSTRAP=localhost:9092 REDIS_HOST=localhost
export PUBLIC_URL="${PUBLIC_URL:-https://${SPACE_HOST:-localhost:7860}/match}"
JVM="-Xmx768m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

DB_URL="$CORE_DB_URL" java $JVM -jar /app/core.jar &
DB_URL="$INGEST_DB_URL" java $JVM -jar /app/ingest.jar &
DB_URL="$MATCH_DB_URL" CORE_API_URL=http://localhost:8080 java $JVM -jar /app/match.jar &

caddy run --config /app/Caddyfile --adapter caddyfile &

wait -n
echo "a process exited, restarting container"
exit 1
