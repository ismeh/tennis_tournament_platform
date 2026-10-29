# AGENTS.md

PuntoMatch — a Master's thesis project (TFM). Java/Spring Boot backend plus an
Angular frontend.

Verified against the code on 2026-10-27. Where this file and `docs/` disagree, this file reflects
the current code.

## Repository shape

One git repository with two independent builds. There is no root task runner, no CI pipeline, and
no Docker Compose file for the application itself.

```
backend/    Java 25, Spring Boot 4.0, Maven (wrapper at ./mvnw)
frontend/   Angular 20, SSR, zoneless, TailwindCSS 3
docs/       architecture, api-spec, coding-standards, domain-model
```

`backend/compose.yaml` provisions **PostgreSQL and pgAdmin only** — it does not build or run the
application.

## Environment notes

- `pom.xml` requires **JDK 25**. It is not installed system-wide on this host, and `JAVA_HOME` is
  unset. Export it before invoking Maven:
  `export JAVA_HOME=/path/to/jdk-25 && export PATH="$JAVA_HOME/bin:$PATH"`.
- `node_modules/` is not committed (`package-lock.json` is gitignored). Run `npm install` first.
- The frontend reads its backend URL from `src/app/shared/constants.ts`, not from
  `src/environments/`. Those files only carry the request-logger configuration.

## Commands

### Backend (`cd backend`)

```bash
export JAVA_HOME=/path/to/jdk-25
./mvnw spring-boot:run                  # dev profile, port 8085
./mvnw test
./mvnw test -Dtest=TournamentServiceTest
./mvnw test -Dtest=TournamentServiceTest#should_create_tournament_with_authenticated_creator
./mvnw compile
```

No formatter, linter or static-analysis plugin is configured; match the surrounding style by hand.

### Frontend (`cd frontend`)

```bash
npm install
npm start                               # ng serve, port 4200
npm run build                           # production build with SSR
npm run test:headless                   # single run; plain `npm test` stays in watch mode
npm run test:ci                         # single run with coverage
npm run format                          # Prettier: printWidth 100, singleQuote, angular HTML parser
```

There is no `lint` or standalone `typecheck` script. Type errors surface through `npm run build` or
`npx tsc -p tsconfig.app.json --noEmit`.

Karma loads `zone.js` and `zone.js/testing` (declared in the `test` target of `angular.json`) so
`TestBed` works even though the application runs zoneless. If Chrome is not on `PATH`, export
`CHROME_BIN`.

### Database

```bash
cd backend && docker compose up -d       # postgres:5432, pgAdmin:5050
```

Schema and seed data are mounted as `docker-entrypoint-initdb.d`, so they only apply to a fresh
volume. Reset with `docker compose down -v && docker compose up -d`.

Dev profile environment variables (no defaults — startup fails without them):

- `DATASOURCE_URL`, `DB_USER`, `DB_PASSWORD` — read in `application-dev.yaml`
- `JWT_SECRET_KEY` — has a development default in `application.yaml`

`application-dev.yaml` sets `database-platform` to the H2 dialect while the configured driver is
PostgreSQL, and writes to `logs/prod-application.log`. Both are inherited from an earlier setup;
align them with the actual database when touching that file.

## Architecture

### Backend — hexagonal, dependency rule `domain ← application ← infrastructure`

Inner layers must not import outer ones.

```
domain/           models/, models/enums/, events/, exceptions/, port/in/, port/out/
application/      services/           -> TournamentService, AuthService, MemberService
                  service/            -> EventService
                  services_use_cases/ -> interface-only placeholder for the auth use cases
infrastructure/   controller/, controller/dto/, controller/mapper/,
                  persistence/{entity,repository,adapter,mapper}/, security/
```

Ports are `domain/port/in/` (use cases) and `domain/port/out/` (repositories). An older revision of
`docs/architecture.md` referred to `domain/contracts/`; the current location is `domain/port/`.

Adding a feature: port in `domain/port/out/` → adapter in `persistence/adapter/` → service in
`application/services/` → controller and DTO in `infrastructure/controller/`. Map across layer
boundaries with MapStruct (`*WebMapper`, `*EntityMapper`), not by hand.

Two names are worth knowing before you search for them:

- The JPA entity for a member maps to the **`users`** table (`MemberEntity`), not `members`.
- `TournamentEntity` calls the tournament name `formalName` and flattens `playPeriod` into
  `playStartDate` / `playEndDate`; `Tournament` keeps them as a `TournamentPeriod` value object.

Unused leftovers to leave alone unless the task is cleanup:

- `application/services_use_cases/AuthService.java` — package placeholder, not wired.
- `frontend/src/app/components/login.ts` — an earlier `LoginComponent` (`app-login`). The routed
  one is `features/auth/login.ts` (`app-login-page`); import that one.

### Frontend — feature-based standalone components

- All components are `standalone: true`; dependency injection uses `inject()`, never constructor
  parameters.
- Change detection is zoneless (`provideZonelessChangeDetection()`): nothing repaints unless you use
  a signal or the `async` pipe. `zone.js` is present in `package.json` for the test runner only.
- SSR is enabled (`outputMode: server`). Guard browser-only APIs with
  `isPlatformBrowser(this.platformId)`.
- Routes are declared together in `app/app.routes.ts`; there are no lazy feature route files. Paths
  are Spanish (`/torneos`, `/torneos/crear`, `/torneos/:id`), except the auth routes (`/login`,
  `/register`).
- HTTP services live in `app/data/services/`, return `Observable`s and do not subscribe internally.

## Conventions that differ from framework defaults

- **Code identifiers in English; UI strings, routes and domain vocabulary in Spanish.**
- **Backend test methods use `snake_case`** — for example
  `should_create_tournament_with_authenticated_creator`. This is intentional.
- `@RequiredArgsConstructor` for constructor injection; `@Builder` for domain models; Java `record`
  for DTOs.
- Prettier configuration lives in the `prettier` key of `package.json`, not a `.prettierrc`.
- `develop` is the working branch, `main` is the default. Commits use the imperative, present tense.

## API surface status

`docs/api-spec.md` documents the implemented endpoints and marks the ones still planned. In
particular:

| Endpoint the frontend references | Backend status |
|---|---|
| `GET /api/age-categories` | planned |
| `POST /api/tournaments/{id}/inscriptions` | planned |
| `GET /members/{email}` | planned (no `MemberController` yet) |

Two smaller inconsistencies to be aware of:

- `POST /api/tournaments/{id}/events` returns a `TournamentResponse`; the Angular service types the
  response as `void`.
- `GET`/`PUT /api/auth/profile` are implemented but not yet consumed by `auth.service.ts`.

`GlobalExceptionHandler` returns a structured `ErrorResponse` record. `LoginController` declares its
own local `@ExceptionHandler` methods, which take precedence for that controller and return a plain
string body.

## Further reading

`docs/coding-standards.md` (naming, class layout, transactions), `docs/domain-model.md` (entity map
and schema), `docs/api-spec.md` (endpoints), `docs/architecture.md` (layer diagrams).
