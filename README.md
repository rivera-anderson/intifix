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
│   ├── monolith/       Backend principal (Spring Boot) — submódulo intifix-2026
│   └── admin-django/   API del panel admin (Django + DRF)
├── packages/           Código compartido entre frontends (próximamente)
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

En Windows, `start.bat` levanta todo: monolito (:8080), Django (:8000) y los dos frontends. El gateway se corre aparte desde `services/gateway` y queda en `:8090` (ver su README).

## Arquitectura

Hoy todo pasa por el **gateway**, que reparte las peticiones al monolito y al panel Django. La migración a microservicios va sacando módulos del monolito uno por uno (patrón strangler fig): cada servicio nuevo se agrega como ruta en el gateway y los frontends no cambian.

## Despliegue

Ver [`infra/DEPLOY.md`](infra/DEPLOY.md): VPS con Docker + Caddy para el backend, Vercel para los frontends, bases de datos en la nube.
