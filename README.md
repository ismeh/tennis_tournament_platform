# Tennis Tournament Platform

![Build](https://img.shields.io/badge/build-not%20configured-lightgrey)
![Tests](https://img.shields.io/badge/tests-not%20configured-lightgrey)
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
3. Start all services:
   ```bash
   docker compose up --build
   ```

### Default Local URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| PostgreSQL | localhost:5432 |

### Default Credentials

Sample users are seeded from SQL scripts, but plaintext passwords are not documented in the repository. For local login, use the public registration/login endpoints described in [`/docs/api-spec.md`](./docs/api-spec.md).

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
| [`/compose.yaml`](./compose.yaml) | Local multi-service orchestration (frontend/backend/database) |
| [`/.env.example`](./.env.example) | Environment variable template for local setup |

Component-specific guides:

- Backend: [`/backend/README.md`](./backend/README.md)
- Frontend: [`/frontend/README.md`](./frontend/README.md)

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

- Automated CI badges still show as not configured for build/test workflows
- API coverage is partial; several endpoints are planned but not implemented yet
- Deployment workflow exists but requires repository secrets and AWS setup

## License

This project is licensed under the MIT License. See [`LICENSE`](./LICENSE).
