#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${1:?APP_DIR is required}"
RELEASE_JAR="${2:?RELEASE_JAR is required}"
SERVICE_NAME="${3:?SERVICE_NAME is required}"

RELEASE_PATH="${APP_DIR}/releases/${RELEASE_JAR}"
CURRENT_PATH="${APP_DIR}/app.jar"

if [ ! -f "${RELEASE_PATH}" ]; then
  echo "Release jar not found: ${RELEASE_PATH}" >&2
  exit 1
fi

ln -sfn "${RELEASE_PATH}" "${CURRENT_PATH}"
sudo systemctl restart "${SERVICE_NAME}"

for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:8080/actuator/health" >/dev/null; then
    sudo systemctl is-active --quiet "${SERVICE_NAME}"
    find "${APP_DIR}/releases" -name 'app-*.jar' -type f -printf '%T@ %p\n' \
      | sort -nr \
      | awk 'NR>5 {print $2}' \
      | xargs -r rm -f
    exit 0
  fi
  sleep 2
done

sudo systemctl status "${SERVICE_NAME}" --no-pager --lines=0
exit 1
