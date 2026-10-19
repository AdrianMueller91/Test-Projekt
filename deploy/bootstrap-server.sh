#!/usr/bin/env bash
# Einmalige Einrichtung eines frischen Servers (Debian 13 oder Ubuntu 24.04) für das Test-Projekt.
#
# Aufruf als root auf dem Server, mit der Domain als Argument:
#   bash bootstrap-server.sh demo.fitness-mueller.de
# Das Skript ist wiederholbar; vorhandene Geheimnisse in der .env bleiben erhalten.
#
# Ergebnis:
# - Docker, Firewall (nur 22, 80, 443), automatische Sicherheitsupdates, fail2ban für SSH
# - SSH nur noch mit Schlüssel (sofern root bereits einen Schlüssel hat)
# - Benutzer "deploy" für die Pipeline mit eigenem SSH-Schlüssel unter /root/test-projekt-deploy-key
# - /opt/test-projekt/.env mit zufällig erzeugten Passwörtern
# Geheimnisse gibt das Skript nicht aus; sie stehen nur in der .env und im Schlüssel auf dem Server.
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Bitte als root ausführen." >&2
  exit 1
fi
DOMAIN="${1:?Domain fehlt, z. B. demo.fitness-mueller.de}"
APP_DIR=/opt/test-projekt

echo "== Pakete und Sicherheitsupdates"
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get upgrade -yq
apt-get install -yq ca-certificates curl ufw fail2ban unattended-upgrades openssl
# Sicherheitsupdates täglich automatisch einspielen
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'APT'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT
systemctl enable --now unattended-upgrades

echo "== fail2ban für SSH"
# Debian 13 schreibt kein auth.log mehr, daher Journal als Quelle
cat > /etc/fail2ban/jail.d/sshd.local <<'JAIL'
[sshd]
enabled = true
backend = systemd
maxretry = 5
findtime = 10m
bantime = 1h
JAIL
systemctl enable --now fail2ban
systemctl restart fail2ban

echo "== Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker
# Container-Logs begrenzen, damit die Platte nicht vollläuft
if [[ ! -f /etc/docker/daemon.json ]]; then
  cat > /etc/docker/daemon.json <<'JSON'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
JSON
  systemctl restart docker
fi

echo "== Firewall"
# Hinweis: Von Docker veröffentlichte Ports umgehen ufw. Veröffentlicht werden nur 80 und 443 (Caddy).
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable

echo "== Benutzer deploy"
if ! id deploy >/dev/null 2>&1; then
  useradd --create-home --shell /bin/bash deploy
fi
usermod -aG docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
KEY=/root/test-projekt-deploy-key
if [[ ! -f $KEY ]]; then
  ssh-keygen -t ed25519 -N "" -C "github-actions-test-projekt" -f "$KEY" >/dev/null
fi
cp "$KEY.pub" /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

echo "== Anwendungsordner und Geheimnisse"
install -d -m 750 -o deploy -g deploy "$APP_DIR"
ENV_FILE="$APP_DIR/.env"
if [[ ! -f $ENV_FILE ]]; then
  cat > "$ENV_FILE" <<ENV
# Erzeugt von bootstrap-server.sh am $(date +%F). Nicht ins Repository übernehmen.
APP_DOMAIN=${DOMAIN}
POSTGRES_PASSWORD=$(openssl rand -hex 24)
APP_USERNAME=adrian
APP_PASSWORD=$(openssl rand -base64 18 | tr -d '/+=' | cut -c1-20)
ENV
  chown deploy:deploy "$ENV_FILE"
  chmod 600 "$ENV_FILE"
else
  echo ".env existiert bereits, bleibt unverändert."
fi

echo "== SSH absichern"
# Passwort-Login nur abschalten, wenn root bereits einen Schlüssel hat; sonst sperrt man sich aus
if [[ -s /root/.ssh/authorized_keys ]]; then
  cat > /etc/ssh/sshd_config.d/10-hardening.conf <<'SSHD'
PermitRootLogin prohibit-password
PasswordAuthentication no
KbdInteractiveAuthentication no
SSHD
  # Die Hauptdatei setzt beide Werte auf yes; die Einträge dort entfernen, damit nichts widerspricht
  sed -i -E 's/^(PermitRootLogin|PasswordAuthentication)\b/#&/' /etc/ssh/sshd_config
  sshd -t
  systemctl reload ssh
  echo "Passwort-Login abgeschaltet."
else
  echo "WARNUNG: root hat keinen SSH-Schlüssel, Passwort-Login bleibt aktiv."
fi

echo "== Fertig. Benutzer deploy, Docker und Firewall sind eingerichtet."
