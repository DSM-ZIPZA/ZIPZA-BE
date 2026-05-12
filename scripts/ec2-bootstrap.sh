#!/usr/bin/env bash
set -euo pipefail

APP_NAME="zipza-be"
APP_USER="zipza"
APP_DIR="/opt/zipza-be"

sudo useradd --system --home "${APP_DIR}" --shell /usr/sbin/nologin "${APP_USER}" 2>/dev/null || true
sudo mkdir -p "${APP_DIR}/releases"
sudo chown -R "${APP_USER}:${APP_USER}" "${APP_DIR}"
sudo chmod 2775 "${APP_DIR}" "${APP_DIR}/releases"

if [ -n "${SUDO_USER:-}" ]; then
  sudo usermod -aG "${APP_USER}" "${SUDO_USER}"
fi

if ! command -v java >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y openjdk-17-jre-headless curl
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y java-17-amazon-corretto-headless curl
  elif command -v yum >/dev/null 2>&1; then
    sudo yum install -y java-17-amazon-corretto-headless curl
  else
    echo "Install Java 17 and curl manually, then rerun this script." >&2
    exit 1
  fi
fi

if [ ! -f "${APP_DIR}/${APP_NAME}.env" ]; then
  sudo install -o "${APP_USER}" -g "${APP_USER}" -m 600 /dev/null "${APP_DIR}/${APP_NAME}.env"
  echo "Created ${APP_DIR}/${APP_NAME}.env. Fill it before starting the service."
fi

sudo cp "infra/systemd/${APP_NAME}.service" "/etc/systemd/system/${APP_NAME}.service"
sudo systemctl daemon-reload
sudo systemctl enable "${APP_NAME}"

echo "Bootstrap complete. Configure ${APP_DIR}/${APP_NAME}.env, then deploy from GitHub Actions."
