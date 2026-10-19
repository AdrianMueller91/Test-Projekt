#!/usr/bin/env bash
# Läuft auf dem Server, aufgerufen von der Pipeline: lädt das neue Image, startet es und prüft den Health-Check.
# Schlägt der Check fehl, wird automatisch auf das vorherige Image zurückgesetzt.
set -euo pipefail
cd /opt/test-projekt
SHA="${1:?Commit-SHA fehlt}"

echo "Lade Image ${SHA} ..."
gunzip -c image.tar.gz | docker load
rm -f image.tar.gz

# Vorheriges Image für einen schnellen Rückweg aufheben
if docker image inspect test-projekt:current >/dev/null 2>&1; then
  docker tag test-projekt:current test-projekt:previous
fi
docker tag "test-projekt:${SHA}" test-projekt:current

docker compose up -d --remove-orphans

healthy() {
  # Caddy-Container erreicht die App intern; wget ist im Caddy-Image enthalten
  docker compose exec -T caddy wget -qO- http://app:8080/actuator/health 2>/dev/null | grep -q '"UP"'
}

for i in $(seq 1 60); do
  if healthy; then
    echo "Health-Check ok nach $((i * 3)) s"
    # Alte Commit-Tags entfernen; current und previous bleiben
    docker images test-projekt --format '{{.Tag}}' | grep -vE '^(current|previous)$' \
      | xargs -r -I{} docker rmi "test-projekt:{}" >/dev/null 2>&1 || true
    docker image prune -f >/dev/null
    exit 0
  fi
  sleep 3
done

echo "Health-Check fehlgeschlagen. Letzte Log-Zeilen:"
docker compose logs --tail 80 app || true
if docker image inspect test-projekt:previous >/dev/null 2>&1; then
  echo "Setze auf das vorherige Image zurück ..."
  docker tag test-projekt:previous test-projekt:current
  docker compose up -d
fi
exit 1
