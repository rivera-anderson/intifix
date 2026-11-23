# packages/

Código compartido entre los frontends del monorepo. Cada carpeta es un workspace de npm.

Planeados:

- `ui/`: design system compartido (botones, inputs, tokens de color y tipografía).
- `api-client/`: cliente TypeScript generado desde el OpenAPI del gateway.
- `config/`: configuraciones compartidas de ESLint, TypeScript y Tailwind.

Para crear uno: `mkdir packages/ui`, dentro `npm init -y`, ponerle `"name": "@intifix/ui"` y usarlo desde una app con `"@intifix/ui": "*"` en sus dependencias.
