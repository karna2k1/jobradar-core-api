#!/usr/bin/env bash
# Pull the latest code of the three backends and (re)start the stack. Safe to run repeatedly.
set -euo pipefail
cd "$(dirname "$0")"
[ -f .env.prod ] || { echo "Missing .env.prod (copy .env.prod.example)"; exit 1; }

for r in jobradar-core-api job-ingest-service match-notify-service; do
  git -C "../../../$r" pull --ff-only
done

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build --remove-orphans
docker image prune -f >/dev/null

set -a; . ./.env.prod; set +a
echo
echo "Deployed. Health checks (certificates can take a minute on first start):"
for h in core ingest match; do
  printf '  https://%s.%s/actuator/health -> ' "$h" "$DOMAIN_SUFFIX"
  curl -s -m 20 "https://$h.$DOMAIN_SUFFIX/actuator/health" || echo "not up yet"
  echo
done
