# Tennis Tournament Platform

A web platform for organising tennis tournaments, built as the author's Master's thesis (TFM).

- **Backend** — Java 25, Spring Boot 4.0, Maven, PostgreSQL, JWT authentication
- **Frontend** — Angular 20 (SSR, zoneless), TailwindCSS, Angular Material

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 25 | `pom.xml` sets `<java.version>25</java.version>` |
| Node.js | 20.19+ / 22.12+ / 24 | Angular 20 requirement |
| npm | 11.x | |
| Docker | with Compose v2 | runs PostgreSQL and pgAdmin |

## Getting started

### 1. Database

```bash
cd backend
docker compose up -d
```

Starts PostgreSQL 18 on `localhost:5432` (user `postgres-tfm`, password `secret`, database
`demo-tfm`) and pgAdmin on `localhost:5050` (`demo@example.com` / `secret`).

Schema and sample data are mounted under `docker-entrypoint-initdb.d`, so they are applied only to
a **fresh** volume. To rebuild the data:

```bash
docker compose down -v && docker compose up -d
```

### 2. Backend

```bash
cd backend
export JAVA_HOME=/path/to/jdk-25
export DATASOURCE_URL=jdbc:postgresql://localhost:5432/demo-tfm
export DB_USER=postgres-tfm
export DB_PASSWORD=secret

./mvnw spring-boot:run     # http://localhost:8085
```

### 3. Frontend

```bash
cd frontend
npm install
npm start                  # http://localhost:4200
```

The backend base URL is configured in `src/app/shared/constants.ts` (`AppSettings.API_URL`), set to
`http://localhost:8085/api` for local development.

## Commands

**Backend**

```bash
./mvnw test
./mvnw test -Dtest=TournamentServiceTest
./mvnw test -Dtest=TournamentServiceTest#should_create_tournament_with_authenticated_creator
./mvnw compile
```

**Frontend**

```bash
npm run build              # production build with SSR
npm run test:headless      # single run; `npm test` stays in watch mode
npm run test:ci            # single run with coverage
npm run format             # Prettier
```

`test:headless` and `test:ci` use the `ChromeHeadlessNoSandbox` launcher. Set `CHROME_BIN` if
Chrome is not on `PATH`.

## Documentation

| Document | Content |
|---|---|
| [`AGENTS.md`](./AGENTS.md) | Working guide for AI agents: commands, layout, conventions |
| [`docs/architecture.md`](./docs/architecture.md) | Hexagonal backend and frontend layering |
| [`docs/domain-model.md`](./docs/domain-model.md) | Entities, relationships and enums |
| [`docs/api-spec.md`](./docs/api-spec.md) | REST API reference |
| [`docs/coding-standards.md`](./docs/coding-standards.md) | Code conventions |

## Conventions

- **Identifiers in English**; **UI strings, routes and domain vocabulary in Spanish**.
- Backend test methods use **snake_case** (`should_create_tournament_when_found`).
- Hexagonal backend: `domain ← application ← infrastructure`. Ports live in `domain/port/in/` and
  `domain/port/out/`.
- Frontend: standalone components, dependency injection via `inject()`, zoneless change detection
  (signals and the `async` pipe), SSR enabled.
- Branches: `develop` is the working branch, `main` is the default. Commits in the imperative mood.
