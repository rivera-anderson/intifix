# IntiFix

Plataforma que conecta clientes con técnicos de hogar. Monorepo con los frontends, el backend y la infraestructura.

```
intifix/
├── apps/
│   ├── web/            App de clientes y técnicos (React + Vite)
│   ├── admin/          Panel de administración (React + Vite)
│   └── android/        App Android (Kotlin, en construcción)
├── services/
│   ├── gateway/        API gateway (Spring Cloud Gateway) — entrada única
│   ├── chat-notif/     Microservicio de chat en tiempo real y notificaciones
│   ├── monolith/       Backend principal (Spring Boot) — submódulo intifix-2026
│   └── admin-django/   API del panel admin (Django + DRF)
├── packages/           Código compartido entre frontends (próximamente)
├── docs/               Contrato de eventos entre servicios (eventos.md)
└── infra/              Docker Compose de producción, Caddy, scripts y guía de despliegue
```

## Clonar

```bash
git clone --recurse-submodules https://github.com/rivera-anderson/intifix.git
```

Si ya lo clonaste sin submódulos: `git submodule update --init`.

## Correr en local

```bash
npm install          # instala los frontends (workspaces)
npm run dev          # web en :5173 y admin en :5174
```

En Windows, `start.bat` levanta todo: monolito (:8080), gateway (:8090), chat-notif (:8091), Django (:8000) y los dos frontends. chat-notif necesita RabbitMQ: `docker run -d --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management`. Los frontends llaman siempre al gateway.

## Arquitectura

Todo entra por el **gateway**, que reparte las peticiones. La migración a microservicios saca módulos del monolito uno por uno (patrón strangler fig): cada servicio nuevo es una ruta más en el gateway y los frontends no cambian.

| Servicio | Responsabilidad | Datos | Estado |
|---|---|---|---|
| gateway | Entrada única, rate limit | Redis | ✅ |
| chat-notif | Chat en tiempo real y notificaciones | MongoDB, Redis | ✅ extraído |
| monolith | Auth, usuarios, técnicos, servicios, pagos, IA, auditoría | PostgreSQL, MongoDB | En migración |
| admin-django | API del panel admin | PostgreSQL, MongoDB | ✅ |

Los servicios se hablan por **RabbitMQ** (eventos) y por una API interna HTTP. El contrato está en [`docs/eventos.md`](docs/eventos.md).

Siguientes a extraer: ai → pagos → técnicos → servicios → auth.

## Despliegue

Ver [`infra/DEPLOY.md`](infra/DEPLOY.md): VPS con Docker + Caddy para el backend, Vercel para los frontends, bases de datos en la nube.
