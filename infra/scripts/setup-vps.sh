#!/usr/bin/env bash
# =============================================================================
# Prepara un VPS Ubuntu 22.04/24.04 recién creado para IntiFix.
# Correr UNA vez como root:  bash setup-vps.sh
# =============================================================================
set -euo pipefail

DEPLOY_USER="${DEPLOY_USER:-deploy}"
REPO_URL="${REPO_URL:-https://github.com/rivera-anderson/intifix.git}"
APP_DIR="/opt/intifix"

echo "==> Actualizando paquetes"
apt-get update -y && apt-get upgrade -y
apt-get install -y ca-certificates curl git ufw fail2ban

echo "==> Instalando Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi

echo "==> Usuario de despliegue: $DEPLOY_USER"
if ! id "$DEPLOY_USER" >/dev/null 2>&1; then
  adduser --disabled-password --gecos "" "$DEPLOY_USER"
fi
usermod -aG docker "$DEPLOY_USER"
mkdir -p "/home/$DEPLOY_USER/.ssh"
if [ -f /root/.ssh/authorized_keys ]; then
  cp /root/.ssh/authorized_keys "/home/$DEPLOY_USER/.ssh/authorized_keys"
fi
chown -R "$DEPLOY_USER:$DEPLOY_USER" "/home/$DEPLOY_USER/.ssh"
chmod 700 "/home/$DEPLOY_USER/.ssh"

echo "==> Firewall: solo SSH, HTTP y HTTPS"
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp
ufw --force enable

echo "==> Swap de 2 GB (dos JVM + Django en un VPS chico lo necesitan)"
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> Clonando el repo en $APP_DIR"
if [ ! -d "$APP_DIR/.git" ]; then
  git clone --recurse-submodules "$REPO_URL" "$APP_DIR"
fi
chown -R "$DEPLOY_USER:$DEPLOY_USER" "$APP_DIR"

echo
echo "Listo. Siguientes pasos (ver infra/DEPLOY.md):"
echo "  1. su - $DEPLOY_USER"
echo "  2. cd $APP_DIR/infra && cp .env.example .env && nano .env"
echo "  3. ./scripts/deploy.sh"
