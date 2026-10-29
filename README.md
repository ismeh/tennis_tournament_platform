# PuntoMatch

**Every tennis tournament, in one place.**

Spreadsheets, WhatsApp groups, a sign-up sheet taped to the clubhouse door. That is how most tennis
tournaments are still run. **PuntoMatch** replaces all of it with a single, clean workspace where any
club or organiser can publish a tournament, configure its draws and take it from draft to open
inscriptions.

PuntoMatch was presented as a **Master's Final Project (TFM)** for the Master's Degree in
Engineering.

![PuntoMatch home page](docs/screenshots/home.png)

## Why organisers choose PuntoMatch

- **Publish a tournament in minutes.** Name it, pick the surface, set the venue and the dates, say
  how many players fit. PuntoMatch takes care of the rest: the inscription window, the competition
  state and who owns the tournament.
- **One tournament, many draws.** A single event can host as many categories as you need — men's
  singles, women's singles, mixed, U16… — each one with its own draw, all under the same tournament.
- **Never lose track of where you are.** Every tournament moves through a clear lifecycle
  (_draft → open → closed → in progress → completed_), and only the transitions that make sense are
  offered. No accidental re-openings, no mismatched states.
- **Each player has a place.** Players sign up, sign in and come back whenever they want to see
  what is on. Every account is linked to a real person: name, nationality, birth date and RFET
  federation licence.
- **Made for Spanish tennis.** The whole experience is in Spanish — from the routes to the labels —
  so club staff and players feel at home from the first click.
- **Start small, grow later.** The same application runs a weekend club tournament or a seasonal
  circuit, and the API is ready to plug into rankings, brackets and live results as the project
  grows.

## Screenshots

| Tournaments list | Tournament detail |
|---|---|
| ![Tournaments list](docs/screenshots/tournaments.png) | ![Tournament detail](docs/screenshots/tournament-detail.png) |

**Create a tournament** — dates, surface, capacity and the draws it will host:

![Create tournament](docs/screenshots/tournament-create.png)

| Sign in | Create an account |
|---|---|
| ![Login](docs/screenshots/login.png) | ![Register](docs/screenshots/register.png) |

> Screenshots taken from a local run with the sample data shipped in
> `backend/src/main/resources/sql_scripts/data-postgre.sql`.

## Try it in five minutes

### What you need

| Tool | Version | Notes |
|---|---|---|
| JDK | 25 | `pom.xml` sets `<java.version>25</java.version>` |
| Node.js | 20.19+ / 22.12+ / 24 | Angular 20 requirement |
| npm | 11.x | |
| Docker | with Compose v2 | runs PostgreSQL and pgAdmin |

### 1. Start the database

```bash
cd backend
docker compose up -d
```

This brings up PostgreSQL 18 on `localhost:5432` (user `postgres-tfm`, password `secret`, database
`demo-tfm`) and pgAdmin on `localhost:5050` (`demo@example.com` / `secret`).

Schema and sample data are mounted under `docker-entrypoint-initdb.d`, so they are applied only to
a **fresh** volume. To start over with clean data:

```bash
docker compose down -v && docker compose up -d
```

### 2. Start the API

```bash
cd backend
export JAVA_HOME=/path/to/jdk-25
export DATASOURCE_URL=jdbc:postgresql://localhost:5432/demo-tfm
export DB_USER=postgres-tfm
export DB_PASSWORD=secret

./mvnw spring-boot:run     # http://localhost:8085
```

### 3. Open PuntoMatch

```bash
cd frontend
npm install
npm start                  # http://localhost:4200
```

The API base URL is configured in `src/app/shared/constants.ts` (`AppSettings.API_URL`), set to
`http://localhost:8085/api` for local development.

### Sign in with a demo account

The sample data creates one account per player, all with the password `password`:

| Email | Tier |
|---|---|
| `rafa@example.com` | `ADVANCED` |
| `roger@example.com` | `ADVANCED` |
| `serena@example.com` | `ADVANCED` |
| `iga@example.com` | `ADVANCED` |
| `novak@example.com` | `ADVANCED` |
| `coco@example.com` | `INTERMEDIATE` |

Signing in and opening _Torneos_ is enough to see the application working end to end; the list is
only visible to authenticated users.

---

## Under the hood

Everything below is for developers and reviewers.

### Stack

| Layer | Stack |
|---|---|
| Backend | Java 25, Spring Boot 4.0, Spring Security (JWT), Spring Data JPA, MapStruct, PostgreSQL 18 |
| Frontend | Angular 20 (standalone components, SSR, zoneless change detection), TailwindCSS 3, Angular Material |
| Tooling | Maven wrapper, npm, Docker Compose (database only), Karma + Jasmine, JUnit 5 + Mockito |

```
backend/    Java 25 + Spring Boot API (hexagonal: domain ← application ← infrastructure)
frontend/   Angular 20 SPA with SSR
docs/       architecture, domain model, API spec, coding standards, screenshots
```

### Backend

Hexagonal architecture, with the dependency rule `domain ← application ← infrastructure`:

- `domain/port/in/` — use cases, `domain/port/out/` — repository ports
- `application/services/` — `TournamentService`, `AuthService`, `MemberService`
- `infrastructure/` — REST controllers and DTOs, JPA entities, adapters, mappers, security

The flow for a new feature is: port out → persistence adapter → service → controller + DTO, mapping
across layer boundaries with MapStruct.

### Frontend

Feature-based standalone components, routed from `frontend/src/app/app.routes.ts`:

| Route | Page |
|---|---|
| `/` | Landing page |
| `/torneos` | Tournament list (requires session) |
| `/torneos/crear` | Tournament creation form with event selection |
| `/torneos/:id` | Tournament detail: overview, status, inscriptions |
| `/login`, `/register` | Authentication |
| `/como-funciona`, `/contacto` | Placeholder pages |

HTTP services live in `app/data/services/`, return `Observable`s and never subscribe internally.
Change detection is zoneless: the views update through signals and the `async` pipe.

### Commands

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

### Documentation

| Document | Content |
|---|---|
| [`docs/architecture.md`](./docs/architecture.md) | Hexagonal backend and frontend layering |
| [`docs/domain-model.md`](./docs/domain-model.md) | Entities, relationships and enums |
| [`docs/api-spec.md`](./docs/api-spec.md) | REST API reference |
| [`docs/coding-standards.md`](./docs/coding-standards.md) | Code conventions |
| [`AGENTS.md`](./AGENTS.md) | Working guide for AI agents: commands, layout, conventions |

### Conventions

- **Identifiers in English**; **UI strings, routes and domain vocabulary in Spanish**.
- Backend test methods use **snake_case** (`should_create_tournament_when_found`).
- `@RequiredArgsConstructor` for constructor injection, `@Builder` for domain models, `record` for
  DTOs.
- Frontend: standalone components, dependency injection via `inject()`, zoneless change detection,
  SSR enabled.
- Branches: `develop` is the working branch, `main` is the default. Commits in the imperative mood.

### Project status

The core tournament flow is implemented. These endpoints are referenced by the frontend but not
implemented by the backend yet, so the screens that depend on them are incomplete:

| Endpoint | Screen affected |
|---|---|
| `GET /api/age-categories` | Event catalogue, on the creation form and the detail page (shows an error) |
| `POST /api/tournaments/{id}/inscriptions` | Inscriptions tab, on the tournament detail |
| `GET /members/{email}` | Member lookup used by the detail page to resolve the current member (fails silently) |

`GET`/`PUT /api/auth/profile` exist in the backend but are not consumed by the Angular client yet.

### License

Released under the [MIT License](./LICENSE).
