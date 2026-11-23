# chat-notif-service

Primer microservicio extraído del monolito de IntiFix. Se encarga de:

- **Chat en tiempo real**: conversaciones, mensajes, adjuntos, leído/entregado, "escribiendo…" y presencia, por STOMP sobre WebSocket (`/ws/chat`) y REST (`/api/v1/chat/**`).
- **Notificaciones**: bandeja del usuario (`/api/v1/notifications/**`) y envío en tiempo real a `/user/queue/notifications`.

Los endpoints, los DTOs y los destinos STOMP son **idénticos** a los que tenía el monolito, así que los frontends no cambian: el gateway manda esas rutas aquí.

## Cómo se comunica

```
                 REST + WebSocket (vía gateway)
  frontends  ───────────────────────────────►  chat-notif-service ──► MongoDB (conversaciones,
                                                    │    ▲                       mensajes, notificaciones)
          HTTP interno /internal/v1/** (API key)    │    │                  Redis (presencia)
                       ┌────────────────────────────┘    │
                       ▼                                 │
                   monolito ──── RabbitMQ (intifix.events) ────┘
                       ▲    technician.approved/rejected, certificate.approved/rejected
                       └─── chat.message.sent, chat.session.connected/disconnected (auditoría)
```

- **Síncrono (HTTP)**: para validar que un usuario existe y está activo, y para saber quién es el cliente y el técnico de un servicio, llama a la API interna del monolito. El estado de la cuenta se cachea 30 s (`ESTADO_CACHE_TTL`).
- **Asíncrono (RabbitMQ)**: recibe los eventos de técnicos para crear notificaciones y publica la actividad del chat para que el monolito la audite. Contrato completo en [`docs/eventos.md`](../../docs/eventos.md).

## Seguridad

- Valida el mismo JWT que firma el monolito (`JWT_SECRET` compartido). No emite tokens.
- Igual que el monolito, rechaza cuentas baneadas o suspendidas. Si no puede confirmar el estado con el monolito, **niega el acceso** (falla cerrado).
- El handshake de `/ws/**` es público; el JWT se valida en el `CONNECT` de STOMP.

## Correr en local

Necesita el monolito en `:8080`, MongoDB, Redis y RabbitMQ. Lo más fácil es levantar RabbitMQ con Docker:

```bash
docker run -d --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Luego, con las variables puestas (mismo `JWT_SECRET` e `INTERNAL_API_KEY` que el monolito):

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Queda en `http://localhost:8091`. Los frontends deben apuntar al gateway (`:8090`), que reparte entre el monolito y este servicio.

## Variables de entorno

| Variable | Uso |
|---|---|
| `PORT` | Puerto (por defecto `8091`) |
| `MONGO_URI` o `DB_MONGO_*` | MongoDB (por defecto la misma base que usaba el monolito) |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`, `REDIS_SSL` | Presencia online/offline |
| `RABBITMQ_URL` | Por ejemplo `amqps://user:pass@host/vhost` (CloudAMQP) |
| `JWT_SECRET` | El mismo del monolito |
| `MONOLITH_URL` | URL interna del monolito |
| `INTERNAL_API_KEY` | Clave compartida con el monolito para `/internal/v1/**` |
| `CORS_ORIGIN_1..3` | Orígenes de los frontends |
| `SWAGGER_ENABLED` | `true` para ver `/swagger-ui.html` |

## Migración de datos

Por ahora usa la misma base Mongo que el monolito, así no se pierden las conversaciones ni las notificaciones. La diferencia es de **dueño**: desde esta versión, las colecciones `conversaciones`, `mensajes` y `notificaciones` solo las toca este servicio.

Cuando quieras separar la base física:

```bash
for c in conversaciones mensajes notificaciones; do
  mongodump --uri "$MONGO_ORIGEN" --collection "$c"
done
mongorestore --uri "$MONGO_DESTINO" dump/
```

y cambia `MONGO_URI` a la base nueva.
