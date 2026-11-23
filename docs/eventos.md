# Contrato de eventos entre servicios

Todos los eventos viajan por RabbitMQ en el exchange **topic** `intifix.events`, como JSON. Cada servicio declara sus propias colas y tiene **su propia copia** de los records de los eventos: no hay librería compartida, el contrato es este documento.

Reglas:

- Un mensaje que falla 3 veces va a la cola de dead-letter del servicio que lo consume (`<servicio>.dead-letter`), vía el exchange `intifix.events.dlx`. Nunca se reintenta para siempre.
- Cambiar un evento = agregar campos opcionales. Si hay que romper el formato, se crea una routing key nueva (por ejemplo `technician.approved.v2`) y se mantienen las dos un tiempo.
- El tipo se deduce del parámetro del `@RabbitListener`; los mensajes no llevan nombres de clase Java.

## Eventos

| Routing key | Publica | Consume | Payload |
|---|---|---|---|
| `technician.approved` | monolito (technicians) | chat-notif → notificación al técnico | `{ "technicianId": uuid, "approvedBy": uuid }` |
| `technician.rejected` | monolito (technicians) | chat-notif → notificación al técnico | `{ "technicianId": uuid, "rejectedBy": uuid }` |
| `certificate.approved` | monolito (technicians) | chat-notif → notificación al técnico | `{ "idTecnico": uuid, "idEspecialidad": uuid, "nombreEspecialidad": string }` |
| `certificate.rejected` | monolito (technicians) | chat-notif → notificación al técnico | `{ "idTecnico": uuid, "idEspecialidad": uuid, "nombreEspecialidad": string }` |
| `chat.message.sent` | chat-notif | monolito (audit → `websocket_logs`) | `{ "conversationId": uuid, "senderId": uuid, "recipientId": uuid, "messageId": uuid }` |
| `chat.session.connected` | chat-notif | monolito (audit) | `{ "userId": uuid }` |
| `chat.session.disconnected` | chat-notif | monolito (audit) | `{ "userId": uuid }` |

## Colas

| Servicio | Cola | Routing key |
|---|---|---|
| chat-notif | `chat-notif.technician.approved` | `technician.approved` |
| chat-notif | `chat-notif.technician.rejected` | `technician.rejected` |
| chat-notif | `chat-notif.certificate.approved` | `certificate.approved` |
| chat-notif | `chat-notif.certificate.rejected` | `certificate.rejected` |
| monolito | `monolith.audit.chat.message.sent` | `chat.message.sent` |
| monolito | `monolith.audit.chat.session.connected` | `chat.session.connected` |
| monolito | `monolith.audit.chat.session.disconnected` | `chat.session.disconnected` |

## API interna (síncrona)

Además de los eventos, los microservicios consultan al monolito por HTTP. Estas rutas **no pasan por el gateway** (solo red interna de Docker) y exigen la cabecera `X-Internal-Api-Key` = `INTERNAL_API_KEY`.

| Método y ruta | Respuesta |
|---|---|
| `GET /internal/v1/usuarios/{id}/estado` | `{ "existe": bool, "activo": bool }` |
| `GET /internal/v1/servicios/{id}/participantes` | `{ "idServicio", "idCliente", "idTecnico" }` o 404 |
| `GET /internal/v1/catalogo/categorias` | `["Refrigeradoras", ...]` (solo con técnicos aprobados) |
| `GET /internal/v1/catalogo/tecnicos?categoria=` | Técnicos aprobados de la especialidad más parecida, con reputación y `urlPerfil` |
| `GET /internal/v1/catalogo/servicios?titulo=` | Hasta 10 servicios cuyo título coincide |
| `GET /internal/v1/clientes/{id}` | Datos del cliente o 404 |
| `GET /internal/v1/clientes/{id}/servicios` | Hasta 10 servicios recientes del cliente |

Quién llama a qué: chat-notif usa `usuarios/estado` y `servicios/participantes`; ai-service usa `usuarios/estado`, `catalogo/*` y `clientes/*`. ai-service no publica ni consume eventos.

## Pendiente

- **Outbox**: hoy, si RabbitMQ está caído cuando el monolito aprueba a un técnico, el evento se pierde (queda en el log). La solución es guardar el evento en una tabla en la misma transacción y publicarlo después.
