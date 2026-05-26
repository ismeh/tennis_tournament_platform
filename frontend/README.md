# Frontend - Tennis Tournament Platform

Aplicación web en Angular 20 con SSR (Server-Side Rendering) para la plataforma de torneos de tenis.

## Requisitos

- Node.js 20+
- npm 10+

Comprobar versiones:

```bash
node -v
npm -v
```

## Instalación

Desde la carpeta `frontend/`:

```bash
npm install
```

## Puesta en marcha en desarrollo

1. Arrancar el frontend:

```bash
npm run start
```

2. Abrir en navegador:

- `http://localhost:4200`

Notas:

- `start` usa la configuración `development` (definida en `angular.json`).
- El frontend lee la API desde `public/config.json` en runtime.
- Puedes usar `http://localhost:8080/api` para desarrollo local, sin recompilar Angular para cambiarlo.

## Build y ejecución en producción

### 1) Generar build de producción

```bash
npm run build
```

Esto genera los artefactos en `dist/tfm_front/`.

### 2) Ejecutar servidor SSR de producción

```bash
npm run serve:ssr:tfm_front
```

Por defecto escucha en `http://localhost:4000`.

Para cambiar el puerto:

```bash
PORT=8080 npm run serve:ssr:tfm_front
```

## Entornos y configuración de logs

Se usa el enfoque nativo de Angular con archivos de entorno:

- Producción: `src/environments/environment.ts`
- Desarrollo: `src/environments/environment.development.ts`

Configuración actual del logger:

- Desarrollo:
	- `enableConsole: true`
	- `minLogLevel: INFO`
- Producción:
	- `enableConsole: false`
	- `minLogLevel: ERROR`

El reemplazo de archivos por entorno está configurado en `angular.json` con `fileReplacements` para `development`.

## Configuración runtime de API (local + Docker)

El frontend lee la URL de API en runtime desde `public/config.json`:

```json
{
	"apiUrl": "http://localhost:8080/api",
	"production": "false"
}
```

- En local (`npm run start`): se usa directamente `public/config.json`.
- En Docker: se usa `public/config.template.json` y el contenedor genera `config.json` al arrancar con variables de entorno (`API_URL`, `PRODUCTION`).

El script de arranque de contenedor está en `scripts/entrypoint.sh`.

## Scripts útiles

```bash
# Servidor de desarrollo
npm run start

# Build de producción
npm run build

# Build en modo desarrollo (watch)
npm run watch

# Tests unitarios
npm run test
```

## Estructura principal

```text
src/app/
	core/         # Auth, interceptores y servicios globales
	data/         # Interfaces y servicios HTTP
	features/     # Páginas funcionales
	components/   # Componentes compartidos de UI
	layout/       # Layout base
	shared/       # Constantes y utilidades reutilizables
```

## Diagrama de arquitectura frontend

![arquitectura-front](docs/images/frontend-data-flow.png)

Resumen del flujo:

1. El router carga la feature según la ruta activa.
2. La feature usa servicios de la capa data para solicitar datos.
3. El interceptor añade token JWT a las peticiones HTTP.
4. La API responde y los datos vuelven al componente para renderizar UI.

## Troubleshooting rápido

- `EADDRINUSE: address already in use`
	- El puerto está ocupado. Cambia el puerto o cierra el proceso previo.
- Errores CORS o llamadas API fallando
	- Verifica que el backend esté activo en el puerto esperado (`8085` en dev).
- Build funciona pero no arranca SSR
	- Ejecuta primero `npm run build` y después `npm run serve:ssr:tfm_front`.

## Mejoras recomendadas para este README

1. Añadir diagrama simple de arquitectura frontend (routing, auth interceptor, capa data).
2. Incluir una sección "Primer flujo funcional" (login -> listado torneos -> detalle).
3. Documentar estrategia de despliegue (PM2, Docker o reverse proxy) para producción real.
4. Añadir tabla de compatibilidad de versiones (Node, Angular CLI, npm).
5. Incluir checklist de validación antes de PR (`npm run build` y `npm run test`).
