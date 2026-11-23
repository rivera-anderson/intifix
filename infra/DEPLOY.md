# Despliegue de IntiFix

Cómo queda en producción:

```
intifix.com, www.intifix.com  →  Vercel  (apps/web)
admin.intifix.com             →  Vercel  (apps/admin)
api.intifix.com               →  VPS → Caddy (HTTPS) → gateway → monolito / chat-notif / ai
admin-api.intifix.com         →  VPS → Caddy (HTTPS) → gateway → Django
Bases de datos                →  Aiven/Neon (Postgres), MongoDB Atlas, Upstash (Redis)
Eventos entre servicios       →  RabbitMQ (contenedor interno del VPS, o CloudAMQP)
```

Si al final usas otro dominio (por ejemplo `intifix.pe`), cámbialo en `DOMAIN` del `.env` y en las variables de Vercel. Nada más está escrito a mano.

## 1. Comprar el dominio

Revisa si `intifix.com` está libre en Cloudflare Registrar o Namecheap. Si está tomado: `intifix.pe` (Punto.pe) o `intifix.app`.

Recomendado: maneja el DNS en **Cloudflare** aunque compres el dominio en otro lado (cambias los nameservers al de Cloudflare).

## 2. Crear el VPS

Cualquier proveedor sirve (Oracle Cloud free ARM, Hetzner, DigitalOcean con créditos del Student Pack). Mínimo **4 GB de RAM**: corren cuatro JVM (gateway, monolito, chat-notif y ai), Django y RabbitMQ. Cada microservicio nuevo suma unos 400-500 MB. Sistema: **Ubuntu 24.04**. Al crearlo, agrega tu clave SSH pública.

Luego, desde tu PC:

```bash
ssh root@IP_DEL_VPS
curl -fsSL https://raw.githubusercontent.com/rivera-anderson/intifix/main/infra/scripts/setup-vps.sh -o setup-vps.sh
bash setup-vps.sh
```

Si el repo es privado, copia el script con `scp` en vez de `curl` y clona con un token.

El script instala Docker, crea el usuario `deploy`, activa el firewall (solo 22, 80 y 443), agrega 2 GB de swap y clona el repo en `/opt/intifix`.

## 3. DNS

En Cloudflare (o donde tengas el DNS):

| Tipo | Nombre | Valor | Proxy de Cloudflare |
|---|---|---|---|
| A | `api` | IP del VPS | Apagado (nube gris) al inicio |
| A | `admin-api` | IP del VPS | Apagado al inicio |
| — | `@`, `www`, `admin` | Los que te indique Vercel en el paso 6 | Apagado |

Deja el proxy de Cloudflare apagado hasta que Caddy saque los certificados. Después puedes prenderlo con SSL en modo **Full (strict)**.

## 4. Variables y primer arranque

```bash
su - deploy
cd /opt/intifix/infra
cp .env.example .env
nano .env        # llena todo: BD, Redis, JWT_SECRET, RABBITMQ_PASSWORD, INTERNAL_API_KEY, Cloudinary, etc.
```

Las imágenes de GHCR se crean la primera vez que corre el workflow **Deploy** (paso 5). Si todavía no existen, construye en el VPS:

```bash
docker compose up -d --build
```

Si ya existen:

```bash
./scripts/deploy.sh
```

Si las imágenes de GHCR son privadas, primero: `echo TOKEN | docker login ghcr.io -u rivera-anderson --password-stdin` (token con permiso `read:packages`), o hazlas públicas en GitHub > Packages.

Comprueba:

```bash
docker compose ps                          # todo "healthy"
curl https://api.intifix.com/actuator/health
docker compose logs -f gateway monolith    # si algo falla
```

## 5. GitHub Actions

- **CI** (`ci.yml`) corre solo: compila frontends, gateway y monolito.
- **Deploy** (`deploy.yml`) construye las imágenes y las sube a GHCR en cada push a `main` que toque `services/` o `infra/`.

Para que además despliegue solo en el VPS, en GitHub > Settings > Secrets and variables > Actions:

- Variable `DEPLOY_ENABLED` = `true`
- Secret `VPS_HOST` = IP del VPS
- Secret `VPS_USER` = `deploy`
- Secret `VPS_SSH_KEY` = una clave privada **nueva** solo para despliegues (su pública va en `/home/deploy/.ssh/authorized_keys`)

Si el repo del monolito (`intifix-2026`) es privado, el checkout del submódulo necesita un token: agrega `token: ${{ secrets.SUBMODULES_TOKEN }}` en los `actions/checkout` y crea ese secret con un token de lectura.

## 6. Frontends en Vercel

Crea **dos proyectos** importando el mismo repo:

| Proyecto | Root Directory | Variables | Dominios |
|---|---|---|---|
| intifix-web | `apps/web` | `VITE_API_BASE_URL=https://api.intifix.com`, `VITE_WS_URL=wss://api.intifix.com/ws/chat` | `intifix.com`, `www.intifix.com` |
| intifix-admin | `apps/admin` | `VITE_API_URL=https://admin-api.intifix.com` | `admin.intifix.com` |

Vercel detecta Vite solo. El `vercel.json` de cada app hace que las rutas del SPA no den 404 al recargar.

## Checklist final

- [ ] `https://api.intifix.com/actuator/health` responde `UP`
- [ ] Login desde `https://intifix.com` funciona (si falla por CORS, revisa `DOMAIN` en `.env`)
- [ ] El chat conecta (WebSocket por `wss://`)
- [ ] `https://admin.intifix.com` entra al panel
- [ ] Subir una foto funciona (Cloudinary configurado)
- [ ] Un push a `main` dispara Deploy y el VPS se actualiza
