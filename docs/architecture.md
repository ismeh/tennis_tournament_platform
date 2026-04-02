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
│   ├── contracts/       Repository port interfaces
│   └── (enums via com.tennis.domain.model.enums for now)
├── application/
│   ├── services/        Business logic orchestrators
│   ├── services_use_cases/  AuthService
│   └── architecture/    Command, Query, UseCase, UseCaseBus interfaces
└── infrastructure/
    ├── controller/      REST controllers + DTOs + WebMappers
    ├── persistence/     JPA entities + JPA repositories + adapters + PersistenceMappers
    ├── security/        SecurityConfig, JwtService, JwtAuthenticationFilter, MemberDetailsService
    └── loggin/          CorrelationIdFilter (MDC correlation ID injection)
```

### Dependency Rule

Inner layers must not depend on outer layers:

```
domain ← application ← infrastructure
```

Cross-boundary communication uses port interfaces (defined in `domain/contracts/`) implemented by infrastructure adapters.

---

## Frontend Architecture

### Layer Diagram

```
app/
├── core/auth/           Authentication: AuthService, authInterceptor (global, singleton)
├── data/
│   ├── interfaces/      TypeScript interfaces matching backend DTOs
│   └── services/        HTTP services (Repository layer)
├── features/            Page-level components (routed)
├── components/          Shared components: Header, Footer (layout-level)
├── layout/              AppLayoutComponent: wraps header + outlet + footer
└── shared/              Constants (AppSettings), pipes, directives
```

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
  /api/login → public
  all other paths → authenticated
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
      → Repository port call (domain/contracts/)
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
