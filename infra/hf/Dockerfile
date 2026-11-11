# All-in-one image for a free Hugging Face Docker Space (2 vCPU / 16 GB, no card needed):
# core-api + job-ingest + match-notify + Kafka (KRaft) + Redis, behind Caddy on port 7860.
# Postgres stays on Neon. The three services are built from their GitHub repos at build time.

FROM maven:3.9-eclipse-temurin-21 AS build
RUN apt-get update -q && apt-get install -y -q --no-install-recommends git && rm -rf /var/lib/apt/lists/*
WORKDIR /src
# bump to force a fresh clone when rebuilding
ARG CACHE_BUST=1
RUN git clone --depth 1 https://github.com/karna2k1/jobradar-core-api.git core \
 && git clone --depth 1 https://github.com/karna2k1/job-ingest-service.git ingest \
 && git clone --depth 1 https://github.com/karna2k1/match-notify-service.git match
RUN for s in core ingest match; do (cd $s && mvn -q -B package -DskipTests); done

FROM apache/kafka:3.9.1 AS kafka
FROM caddy:2 AS caddy

FROM eclipse-temurin:21-jre
RUN apt-get update -q && apt-get install -y -q --no-install-recommends redis-server && rm -rf /var/lib/apt/lists/*
COPY --from=kafka /opt/kafka /opt/kafka
COPY --from=caddy /usr/bin/caddy /usr/bin/caddy
COPY --from=build /src/core/target/*.jar /app/core.jar
COPY --from=build /src/ingest/target/*.jar /app/ingest.jar
COPY --from=build /src/match/target/*.jar /app/match.jar
COPY Caddyfile start.sh /app/
RUN chmod +x /app/start.sh && mkdir -p /data && chown -R 1000:1000 /data /app

# Spaces run as uid 1000; everything writable lives under /data
USER 1000
ENV HOME=/data LOG_DIR=/data/kafka/applogs
EXPOSE 7860
CMD ["/app/start.sh"]
