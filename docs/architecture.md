# Architecture

## Overview

PuntoMatch follows a **hexagonal architecture** (also known as Ports & Adapters) on the backend, and a **feature-based standalone component architecture** on the frontend.

---

## Backend Architecture

### Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Infrastructure                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  REST Layer  │  │  Persistence │  │   Security   │   │
│  │ Controllers  │  │ JPA Entities │  │  JWT Filter  │   │
│  │    DTOs      │  │  Adapters    │  │ SecurityCfg  │   │
│  │  WebMappers  │  │  JPA Repos   │  │  JwtService  │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘   │
│         │                 │                             │
├─────────▼─────────────────▼─────────────────────────────┤
│                    Application                          │
│     TournamentService | AuthService | MemberService      │
│                      | EventService                      │
├─────────────────────────────────────────────────────────┤
│                      Domain                             │
│     Models: Tournament, TournamentPeriod, Event,        │
│             Member, Person                              │
│     Enums: Surface, TournamentStatus, MemberTier         │
│     Ports (in/out): TournamentRepository, etc.           │
│     Exceptions: BaseException + concrete subclasses     │
└─────────────────────────────────────────────────────────┘
```

The lower half of the competition model — stages, draws, matchups and sets — is currently modelled
at the persistence layer (`StageEntity`, `DrawEntity`, `MatchupEntity`, `MatchSetEntity`). Domain
models and services for those concepts are planned.

### Package Structure

```
com.puntomatch
├── domain/
│   ├── models/          Pure domain objects (no framework annotations)
│   │                    Tournament, TournamentPeriod, Event, Member, Person
│   ├── models/enums/    Surface, TournamentStatus, MemberTier, AgeCategoryEnum
│   ├── events/          Domain events (TournamentPublished)
│   ├── exceptions/      BaseException + concrete domain exceptions
│   └── port/
│       ├── in/          Inbound use-case interfaces (CreateTournamentUseCase, ...)
│       └── out/         Outbound repository ports (TournamentRepository, MemberRepository,
│                        PersonRepository)
├── application/
│   ├── services/        TournamentService, AuthService, MemberService
│   ├── service/         EventService
│   └── services_use_cases/  Interface placeholder for the auth use cases
└── infrastructure/
    ├── controller/      REST controllers + dto/ + mapper/ (TournamentWebMapper)
    ├── persistence/     entity/ + repository/ + adapter/ + mapper/
    └── security/        SecurityConfig, JwtService, JwtAuthenticationFilter, MemberDetailsService
```

### Dependency Rule

Inner layers must not depend on outer layers:

```
domain ← application ← infrastructure
```

Cross-boundary communication uses port interfaces (defined in `domain/port/out/`) implemented by
infrastructure adapters. Inbound use cases are declared in `domain/port/in/`.

### Notes on the current layout

- `application/services/` (plural) holds the main services; `application/service/` (singular) holds
  `EventService`. New services belong in `application/services/`.
- `services_use_cases/` is a placeholder package for inbound use-case interfaces; controllers call
  services directly today.
- The event/inscription branch of the model (`StageEntity`, `DrawEntity`, `MatchupEntity`,
  `MatchSetEntity`) exists at the persistence layer only; it has no domain model or service yet.

---

## Frontend Architecture

### Layer Diagram

```
app/
├── core/
│   ├── auth/            AuthService, authInterceptor (function-style, registered in app.config)
│   └── logging/         RequestLoggerService + console/file transports
├── data/
│   ├── interfaces/      TypeScript interfaces matching backend DTOs
│   └── services/        HTTP services (Repository layer)
├── features/            Page-level components (routed): home, auth/, tournaments/
├── components/          Shared components: Header, Footer
├── layout/              AppLayout: wraps header + outlet + footer
└── shared/              constants.ts -> AppSettings
```

### Routing

All routes defined in `app.routes.ts`. No feature routing modules, no lazy `loadChildren`.
SSR is enabled and targets all routes (`outputMode: server`).

Route paths are Spanish (`/torneos`, `/torneos/crear`, `/torneos/:id`, `/como-funciona`,
`/contacto`) except auth, which is English (`/login`, `/register`).

### Change Detection

Zoneless change detection via `provideZonelessChangeDetection()`. Components must trigger updates
through signals or observables with `async` pipe. `zone.js` remains in `package.json` but is not
wired up — do not rely on zone-based change detection.

### API Base URL

`AppSettings.API_URL` in `shared/constants.ts` is a constant (`http://localhost:8085/api`) matching
the backend dev port. It does not read from `environments/`, so changing environments means editing
that constant.

`app.config.ts` reads the request-logger settings from `src/environments/environment`; the
`development` configuration swaps in `environment.development.ts` through the `fileReplacements`
entry in `angular.json`.

---

## Security Architecture

```
Client Request
      │
      ▼
CorsFilter (localhost:* / 127.0.0.1:*, any port, dev only)
      ▼
JwtAuthenticationFilter
  reads Authorization header
  validates JWT signature + expiry
  populates SecurityContext
      │
      ▼
SecurityFilterChain (SecurityConfig)
  OPTIONS /**            → permitAll (CORS preflight)
  /api/auth/login        → permitAll
  /api/auth/register     → permitAll
  /api/auth/refresh      → permitAll
  /api/auth/logout       → permitAll
  everything else        → authenticated
  session policy         → STATELESS
      │
      ▼
Controller → Service → Repository port
```

- Passwords: BCrypt via `BCryptPasswordEncoder`
- Tokens: signed HMAC-SHA256 JWT (jjwt 0.13); refresh tokens are SHA-256 hashed before storage
- Session policy is `STATELESS` — there is no server-side session
- Token source on frontend: `localStorage`, managed exclusively by `AuthService`
  (`access_token`, `refresh_token`, `user_display_name` keys)
- **Correlation ID.** The log patterns in `application-dev.yaml` and `application-prod.yaml`
  interpolate `%X{correlationId}`, but no filter populates that MDC key yet, so the placeholder
  renders empty. Adding a `CorrelationIdFilter` to `infrastructure/` completes this feature.

---

## Data Flow for a Typical Feature

```
Angular Component
   → HTTP service (data/services/)
   → HTTP POST/GET with Bearer token (authInterceptor)
   │
   ▼
Spring Boot Controller (infrastructure/controller/)
   → WebMapper: RequestDTO → Domain Model
   → Service call (application/services/)
      → Repository port call (domain/port/out/)
         → JPA Adapter (infrastructure/persistence/adapter/)
            → JPA Repository → DB
         ← JPA Adapter returns Domain Model
      ← Service returns Domain Model
   → WebMapper: Domain Model → ResponseDTO
   ← ResponseEntity<*Response>
   │
   ▼
Angular Component renders data
```
