# Tennis Tournament Platform

[![Build](https://github.com/ismeh/tennis_tournament_platform/actions/workflows/build.yml/badge.svg)](https://github.com/ismeh/tennis_tournament_platform/actions/workflows/build.yml)
[![Tests](https://github.com/ismeh/tennis_tournament_platform/actions/workflows/test.yml/badge.svg)](https://github.com/ismeh/tennis_tournament_platform/actions/workflows/test.yml)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/ismeh/tennis_tournament_platform)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

Web platform for managing tennis tournaments, participants, and match workflows.

## Purpose

This project provides a full-stack baseline for running tennis competitions with:

- Tournament creation and listing
- Authentication (JWT)
- Participant and event management foundations
- SSR frontend for user-facing flows

## Audience

- Coaches, clubs, and organizers who need tournament operations tooling
- Developers extending a reference full-stack architecture (Angular + Spring Boot + PostgreSQL)

## Key Features

- Angular 20 frontend with SSR
- Spring Boot backend following hexagonal architecture principles
- PostgreSQL persistence (with seed data for local startup)
- Docker Compose orchestration for local environments

## Architecture at a Glance

- **Frontend**: Angular SSR app (`/frontend`)
- **Backend**: Spring Boot REST API (`/backend`)
- **Database**: PostgreSQL container
- **Orchestration**: Docker Compose (`/compose.yaml`)

Detailed architecture: [`/docs/architecture.md`](./docs/architecture.md)

## Quick Start (Docker)

1. Clone the repository:
   ```bash
   git clone https://github.com/ismeh/tennis_tournament_platform.git
   cd tennis_tournament_platform
   ```
2. Create environment variables:
   ```bash
   cp .env.example .env
   ```
3. Start all services using the published backend and frontend images:
   ```bash
   docker compose up
   ```

To build the backend and frontend images from the current local source code instead of pulling the published images, add the local override:

```bash
docker compose -f compose.yaml -f compose.local.yaml up --build
```

In this mode, `compose.yaml` remains the base orchestration file for PostgreSQL, ports, environment variables, dependencies, and published image defaults. `compose.local.yaml` only overrides the backend and frontend services with local Docker builds and `pull_policy: build`, so Docker Compose builds the local images instead of trying to pull `tennis-backend:local` or `tennis-frontend:local` from a registry:

- Backend: `./backend/Dockerfile`
- Frontend: `./frontend/Dockerfile`

For HTTPS local or a public Nginx demo, use the environment matrix in [`/docs/deployment-https.md`](./docs/deployment-https.md#matriz-de-variables-por-modo). The important rule is that `FRONTEND_API_URL` must match the public URL used by the browser: `http://localhost:8080/api` for plain local HTTP, `https://localhost/api` for local Nginx HTTPS, or `https://your-domain/api` for a public demo.

### Default Docker Compose URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| PostgreSQL | localhost:5432 |

For non-Docker local development, the backend PostgreSQL `dev` profile defaults to `http://localhost:8085`, while `devH2` defaults to `http://localhost:8080`, unless `BACKEND_PORT` is set.

### Default Credentials

Sample users are seeded from SQL scripts, but plaintext passwords are not documented in the repository. For local login, use the public registration/login endpoints described in [`/docs/api-spec.md`](./docs/api-spec.md) (`http://localhost:8080` with Docker Compose, `http://localhost:8085` for local PostgreSQL `dev`, or the configured `BACKEND_PORT`).

## Local Development (Without Docker)

### Supported Toolchain Matrix

| Component | Version |
|---|---|
| Java | 25 |
| Maven | 3.8+ |
| Node.js | 24 (recommended) |
| npm | 10+ |
| Angular | 20.x |
| PostgreSQL | 18 (container default) |

### Backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm ci
npm run start
```

## Repository Map

| Path | Ownership / Responsibility |
|---|---|
| [`/backend`](./backend) | Spring Boot API, domain/application/infrastructure layers, persistence, security |
| [`/frontend`](./frontend) | Angular SSR UI, feature routes, auth flows, API client layer |
| [`/docs`](./docs) | Architecture, API specification, domain model, coding standards |
| [`/compose.yaml`](./compose.yaml) | Base multi-service orchestration using published frontend/backend images |
| [`/compose.local.yaml`](./compose.local.yaml) | Docker Compose override that builds frontend/backend images locally |
| [`/.env.example`](./.env.example) | Environment variable template for local setup |

Component-specific guides:

- Backend: [`/backend/README.md`](./backend/README.md)
- Frontend: [`/frontend/README.md`](./frontend/README.md)
- HTTPS local and cheap demo deployment: [`/docs/deployment-https.md`](./docs/deployment-https.md)

## Contributing

### Common Commands

```bash
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend && npm run test:ci
cd frontend && npm run build
cd frontend && npm run format
```

### Branch and PR Workflow

1. Create a branch from `main`
2. Keep changes scoped and test locally
3. Open a PR with a clear summary and checklist
4. Merge only after review and green validations

### Where to Find More Information

- API spec: [`/docs/api-spec.md`](./docs/api-spec.md)
- Architecture: [`/docs/architecture.md`](./docs/architecture.md)
- Troubleshooting: [`/backend/README.md`](./backend/README.md), [`/frontend/README.md`](./frontend/README.md)

## Known Limitations / Roadmap

- API coverage is partial; several endpoints are planned but not implemented yet
- Deployment automation is planned, but repository workflow wiring, secrets, and AWS setup are still required

## License

This project is licensed under the MIT License. See [`LICENSE`](./LICENSE).
