# intifix-gateway

API gateway de IntiFix con Spring Cloud Gateway. Es el único punto de entrada al backend: los frontends llaman a `api.<dominio>` y el gateway decide a qué servicio va cada petición.

## Qué hace hoy

| Ruta | Destino | Rate limit |
|---|---|---|
| Host `admin-api.*` | Panel Django (`ADMIN_URL`) | 10 req/s por IP |
| `/ws/**` | WebSocket de chat-notif-service | — |
| `/api/v1/chat/**`, `/api/v1/notifications/**` | chat-notif-service (`CHAT_NOTIF_URL`) | 30 req/s por IP |
| `/api/v1/ai/**` | ai-service (`AI_URL`) | 1 req/s por IP, ráfaga de 5 (cuesta dinero en OpenAI) |
| `/api/v1/auth/**` | Monolito | 2 req/s por IP (contra fuerza bruta) |
| `/api/**`, `/uploads/**`, Swagger | Monolito (`MONOLITH_URL`) | 20 req/s por IP |

El rate limit usa Redis. Si Redis no responde, el gateway deja pasar las peticiones en vez de cortar el servicio.

La autenticación JWT la sigue validando el monolito. Cuando haya varios microservicios, el siguiente paso es validar el JWT aquí una sola vez.

## Correr en local

Necesita el monolito en `:8080` y, opcionalmente, Redis en `:6379`.

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Queda en `http://localhost:8090`. Para que el frontend pase por el gateway, en `apps/web/.env` pon `VITE_API_BASE_URL=http://localhost:8090` y `VITE_WS_URL=ws://localhost:8090/ws/chat`.

## Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `PORT` | `8090` | Puerto del gateway |
| `MONOLITH_URL` | `http://localhost:8080` | Backend Spring |
| `CHAT_NOTIF_URL` | `http://localhost:8091` | chat-notif-service |
| `CHAT_NOTIF_WS_URL` | `ws://localhost:8091` | WebSocket de chat-notif-service |
| `AI_URL` | `http://localhost:8092` | ai-service |
| `ADMIN_URL` | `http://localhost:8000` | Panel Django |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`, `REDIS_SSL` | localhost, sin SSL | Rate limiter |

## Agregar un microservicio

Pon su ruta en `application.yml` **encima** de la ruta `monolith`, con un `Path` más específico. El monolito deja de recibir esas peticiones y los frontends no se enteran.
