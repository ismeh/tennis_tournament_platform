# Frontend - Tennis Tournament Platform

Angular 20 SSR application for the Tennis Tournament Platform user interface.

## Requirements

- Node.js 24 (recommended)
- npm 10+

Check versions:

```bash
node -v
npm -v
```

## Installation

From the `frontend/` folder:

```bash
npm ci
```

## Run in Development

1. Start the frontend:

```bash
npm run start
```

2. Open in the browser:

- `http://localhost:4200`

Notes:

- `start` uses the `development` configuration defined in `angular.json`.
- The frontend API target is currently `http://localhost:8080/api` (`src/app/shared/constants.ts` and runtime `public/config.json`). If you run backend locally with the default PostgreSQL `dev` profile, either start it with `BACKEND_PORT=8080` or change `public/config.json` to `http://localhost:8085/api`.

## Production Build and SSR Runtime

### 1) Build production assets

```bash
npm run build
```

Artifacts are generated in `dist/tfm_front/`.

### 2) Run SSR server

```bash
npm run serve:ssr:tfm_front
```

Default URL: `http://localhost:4000`

Change port:

```bash
PORT=8080 npm run serve:ssr:tfm_front
```

## Environments and Logging

The project uses native Angular environment files:

- Production: `src/environments/environment.ts`
- Development: `src/environments/environment.development.ts`

Current logger behavior:

- Development:
  - `enableConsole: true`
  - `minLogLevel: INFO`
- Production:
  - `enableConsole: false`
  - `minLogLevel: ERROR`

Environment file replacement is configured in `angular.json` via `fileReplacements`.

## Runtime API Configuration (Local + Docker)

The frontend reads the API URL at runtime from `public/config.json`:

```json
{
	"apiUrl": "http://localhost:8080/api",
	"production": "false"
}
```

- Local (`npm run start`): uses `public/config.json` directly.
- Docker: uses `public/config.template.json` and the container generates `config.json` at startup using environment variables (`API_URL`, `PRODUCTION`).

The container startup script is located at `scripts/entrypoint.sh`.

## Useful Scripts

```bash
# Development server
npm run start

# Production build
npm run build

# Development build with watch
npm run watch

# Unit tests
npm run test

# CI-style tests
npm run test:ci

# Formatting
npm run format
```

## Main Structure

```text
src/app/
  core/         # Authentication, interceptors, global services
  data/         # Interfaces and HTTP services
  features/     # Feature/page components
  components/   # Shared UI components
  layout/       # Base application layout
  shared/       # Constants and reusable utilities
```

## Frontend Architecture

Flow summary:

1. Router loads the active feature view.
2. Feature components call `data/services` for API interactions.
3. Auth interceptor adds JWT tokens to outgoing HTTP requests.
4. API responses are rendered by the component layer.

See full system architecture in [`../docs/architecture.md`](../docs/architecture.md).

## Troubleshooting

- `EADDRINUSE: address already in use`
  - The port is already occupied. Change the port or stop the existing process.
- CORS errors or failing API requests
  - Verify that backend is running on the configured API URL (`http://localhost:8080/api`).
- SSR does not start after a successful build
  - Run `npm run build` first, then `npm run serve:ssr:tfm_front`.
