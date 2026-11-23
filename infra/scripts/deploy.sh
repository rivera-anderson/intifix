#!/usr/bin/env bash
# Actualiza el código y reinicia los contenedores con las imágenes nuevas.
# Lo corre GitHub Actions por SSH, o tú a mano desde el VPS.
set -euo pipefail
cd "$(dirname "$0")/.."

git -C .. pull --ff-only
docker compose pull
docker compose up -d --remove-orphans
docker image prune -f
docker compose ps
