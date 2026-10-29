# AGENTS.md

Tennis Tournament Platform — a Master's thesis project (TFM). Java/Spring Boot backend plus an
Angular frontend.

Verified against the codebase on 2026-10-29. This guide summarizes the development environment,
repository architecture, and project standards.

## Repository shape

One git repository with two independent builds:

```
backend/    Java 25, Spring Boot 4.0, Maven (wrapper at ./mvnw)
frontend/   Angular 20, SSR, zoneless, TailwindCSS 3
docs/       architecture, api-spec, coding-standards, domain-model
CHANGELOG.md  Keep a Changelog 1.1.0 standard
ROADMAP.md    Current, short-, medium-, and long-term milestones
```

`compose.yaml` provisions PostgreSQL, backend, frontend, and reverse proxy for local or server environments.

## Environment notes

- `pom.xml` requires **JDK 25**. If not installed system-wide on the host, set `JAVA_HOME`:
  `export JAVA_HOME=/tmp/opencode/jdk/jdk-25.0.5+8 && export PATH="$JAVA_HOME/bin:$PATH"` (or host path).
- `node_modules/` is not committed. Run `npm install` first in `frontend/`.
- Frontend reads its runtime configuration and API URL according to environment setup.

## Commands

### Backend (`cd backend`)

```bash
export JAVA_HOME=/tmp/opencode/jdk/jdk-25.0.5+8 && export PATH="$JAVA_HOME/bin:$PATH"
./mvnw clean install
./mvnw spring-boot:run                  # port 8080 or BACKEND_PORT
./mvnw test                             # runs all unit & integration tests
./mvnw test -Dtest="*IT"                # integration tests only
```

### Frontend (`cd frontend`)

```bash
npm install
npm run start                           # ng serve, port 4200
npm run build                           # production build with SSR
npm run test:ci                         # single headless test run with coverage
npm run format                          # Prettier
```

## Versioning, Changelog & Roadmap Guidelines (MANDATORY)

To keep changes clean, human-readable, and well-tracked across releases, always follow these rules:

### 1. Changelog Standard (`CHANGELOG.md`)
- Adhere strictly to **[Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)** and **Semantic Versioning**.
- Document **notable, user- or developer-impacting changes** concisely. Do not dump raw git commits or line-by-line diffs.
- Use standard section categories:
  - `Added`: New features or capabilities.
  - `Changed`: Changes in existing functionality.
  - `Deprecated`: Soon-to-be-removed features.
  - `Removed`: Now-removed features.
  - `Fixed`: Bug fixes.
  - `Security`: Vulnerability mitigations.
- Place ongoing work under `## [Unreleased]`. When cutting a release, create a version heading with the date (`## [X.Y.Z] - YYYY-MM-DD`).

### 2. Roadmap Tracking (`ROADMAP.md`)
- `ROADMAP.md` organizes tasks by temporal scope:
  - **In Progress**: Tasks actively being worked on.
  - **Short Term**: Immediate next steps, improvements, and fixes.
  - **Medium Term**: Substantial features, workflows, and tools.
  - **Long Term**: Architectural evolution, multi-tenancy, external federation APIs.
- **Task Lifecycle**: When a task from `ROADMAP.md` is finished and verified:
  1. Remove or check off the item in `ROADMAP.md`.
  2. Add an ultra-summarized, concise bullet in `CHANGELOG.md` under `[Unreleased]`.

## Architecture & Conventions

### Backend — Hexagonal Architecture
- Dependency direction: `domain ← application ← infrastructure`. Inner layers never import outer ones.
- Ports: `domain/port/in/` (use cases) and `domain/port/out/` (repositories & external services).
- Entities & Adapters: `infrastructure/persistence/entity/`, `infrastructure/persistence/adapter/`.
- Mapping across boundaries: MapStruct mappers (`*WebMapper`, `*EntityMapper`).
- Data access: Spring Data JPA + Flyway migrations in `src/main/resources/db/migration/`.

### Frontend — Standalone & Zoneless Angular
- Standalone components (`standalone: true`).
- Dependency injection via `inject()`, avoiding constructor injection.
- Zoneless change detection (`provideZonelessChangeDetection()`): UI updates through Signals and the `async` pipe.
- Routes declared centrally in `src/app/app.routes.ts`.

### Project Conventions
- **Code identifiers in English**; **UI labels, messages, and domain vocabulary in Spanish**.
- Backend test methods use `snake_case` (e.g. `should_return_tournament_when_valid_id`).
- Immutable DTOs via Java `record`. Domain models use Lombok `@Builder`.
