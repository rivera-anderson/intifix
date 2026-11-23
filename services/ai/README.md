# ai-service

Asistente de IntiFix ("Inti"). Segundo microservicio extraído del monolito. Usa **Spring AI + GPT-4o** con Tool Calling para:

- Recomendar técnicos reales según la categoría o el problema que describe el cliente.
- Responder sobre el historial de servicios del propio cliente.
- Estimar un rango de presupuesto para un servicio (`/presupuesto-sugerido`).

Los endpoints son los mismos que tenía el monolito (`/api/v1/ai/**`), así que el frontend no cambia: el gateway manda esas rutas aquí.

## Cómo funciona

```
frontend ──(gateway, rate limit estricto)──► ai-service ──► OpenAI (GPT-4o)
                                                 │  └────► MongoDB (memoria: ai_conversaciones)
                                                 ▼
                              monolito /internal/v1/** (API key)
                              catálogo de técnicos, servicios, datos del cliente
```

Las tools no tocan ninguna base de datos del monolito: le preguntan por HTTP a su API interna. La lógica de "buscar la especialidad más parecida + reputación" sigue en el monolito, que es dueño de esos datos; aquí solo se ordena para recomendar.

## Seguridad (cambios respecto al monolito)

- **El usuario sale del JWT, nunca del body.** Antes, el `userId` del body se pasaba al modelo y las tools aceptaban cualquier UUID: un usuario podía pedir los datos de otro cliente. Ahora las tools del cliente se crean por petición ligadas al usuario autenticado y no reciben ningún id.
- **Cada conversación es del usuario que la creó.** La memoria se guarda como `<idUsuario>:<conversationId>`, así nadie puede leer ni borrar el hilo de otro. Efecto secundario: el historial del asistente guardado antes de esta versión no aparece (es solo la memoria corta del chat).
- Límites de tamaño en el mensaje (4000 caracteres) y en la estimación de presupuesto.
- El gateway limita `/api/v1/ai/**` a 1 petición por segundo por IP (ráfaga de 5), porque cada llamada cuesta dinero en OpenAI.

## Correr en local

Necesita el monolito en `:8080` y MongoDB. Copia `.env.example` a `.env` y llena `OPENAI_API_KEY`, `JWT_SECRET` e `INTERNAL_API_KEY` (los mismos del monolito).

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Queda en `http://localhost:8092`.

## Variables de entorno

| Variable | Uso |
|---|---|
| `PORT` | Puerto (por defecto `8092`) |
| `OPENAI_API_KEY` | Clave de OpenAI |
| `OPENAI_MODEL` | Modelo (por defecto `gpt-4o`) |
| `MONGO_URI` o `DB_MONGO_*` | Memoria de conversaciones |
| `JWT_SECRET` | El mismo del monolito |
| `MONOLITH_URL` | URL interna del monolito |
| `INTERNAL_API_KEY` | Clave compartida para `/internal/v1/**` |
| `CORS_ORIGIN_1..3` | Orígenes de los frontends |
| `SWAGGER_ENABLED` | `true` para ver `/swagger-ui.html` |
