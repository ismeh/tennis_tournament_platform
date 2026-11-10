# Architecture

## Overview

The Tennis Tournament Platform follows a **hexagonal architecture** (also known as Ports & Adapters) on the backend, and a **feature-based standalone component architecture** on the frontend.

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
│           Services  |  Use Cases  |  Auth               │
│   TournamentService | MatchService | MemberService      │
│   AuthService       | UseCaseBus  | CQRS interfaces     │
├─────────────────────────────────────────────────────────┤
│                      Domain                             │
│     Models: Tournament, Match, Member, Ranking...       │
│     Ports (interfaces): TournamentRepository, etc.      │
│     Exceptions: ResourceNotFoundException               │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.tfm.tennis_platform
├── domain/
│   ├── models/          Pure domain objects (no framework annotations)
│   ├── port/            Repository and use-case port interfaces
│   └── exceptions/      Domain exceptions
├── application/
│   ├── services/        Business logic orchestrators
│   └── commands/        Application command DTOs
└── infrastructure/
    ├── controller/      REST controllers + DTOs + WebMappers
    ├── persistence/     JPA entities + JPA repositories + adapters + PersistenceMappers
    ├── security/        SecurityConfig, JwtService, JwtAuthenticationFilter, MemberDetailsService
    ├── pdf/             OpenPDF document exporters
    ├── excel/           Apache POI XLSX exporters
    └── loggin/          CorrelationIdFilter (MDC correlation ID injection)
```

### Dependency Rule

Inner layers must not depend on outer layers:

```
domain ← application ← infrastructure
```

Cross-boundary communication uses port interfaces (defined in `domain/port/`) implemented by infrastructure adapters.

---

## Frontend Architecture

### Layer Diagram

```
app/
├── core/auth/           Authentication: AuthService, authInterceptor (global, singleton)
├── core/offline/        Connectivity signal and IndexedDB snapshot storage
├── core/pwa/            Service worker update lifecycle (SwUpdate wrapper)
├── data/
│   ├── interfaces/      TypeScript interfaces matching backend DTOs
│   └── services/        HTTP services (Repository layer)
├── features/            Page-level components (routed)
├── components/          Shared components: Header, Footer (layout-level)
├── layout/              AppLayoutComponent: wraps header + outlet + footer
└── shared/              Constants (AppSettings), pipes, directives
```

### Progressive Web App and Offline Mode

The frontend is an installable PWA built with `@angular/service-worker`:

- `ngsw-config.json` defines the app shell, lazy static assets and `dataGroups` for public read
  endpoints (tournaments, clubs, rankings, reference data).
- `TournamentOfflineService` stores tournament and inscription snapshots in IndexedDB, used as a
  labelled fallback when a read fails.
- Write operations always require connectivity.
- `ConnectionStatusBannerComponent` surfaces offline, reconnection and update states.
- `PushNotificationService` (in `core/pwa/`) registers Web Push subscriptions through `SwPush`,
  syncs them with `/api/push/subscriptions` and lets the account settings screen enable, disable or
  send a test notification. Push delivery remains tied to the `APP`/`BOTH` channel.

Offline support is deliberately **read-only**; the rationale, alternatives and the Capacitor
evolution path are documented in [`docs/decisions/`](./decisions/README.md).

### Routing

All routes defined in `app.routes.ts`. No feature routing modules.
SSR is enabled and targets all routes (`outputMode: server`).

### Change Detection

Zoneless change detection via `provideZonelessChangeDetection()`. Components must trigger updates through signals or observables with `async` pipe.

---

## Security Architecture

```
Client Request
      │
      ▼
JwtAuthenticationFilter
  reads Authorization header
  validates JWT signature + expiry
  populates SecurityContext
      │
      ▼
SecurityFilterChain (SecurityConfig)
  public endpoints → /api/auth/*, calendar tournament list,
                     public rankings, public age categories,
                     club search/directory/public pages
  other paths      → authenticated
      │
      ▼
Controller → Service → Repository
```

- Passwords: BCrypt via `BCryptPasswordEncoder`
- Tokens: signed HMAC-SHA256 JWT (jjwt 0.13)
- Token source on frontend: `localStorage`, managed exclusively by `AuthService`
- Correlation logging: every request gets a `correlationId` in MDC via `CorrelationIdFilter`

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

## API Namespace Notes

Active REST controllers are namespaced under `/api`, including auth, tournaments, calendar, rankings, pro players, persons, members, matches, clubs, and age categories.

When changing API contracts, update the backend controller, frontend client layer, and API documentation in the same change.
