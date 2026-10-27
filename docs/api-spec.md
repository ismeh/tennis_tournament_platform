# API Specification

Generated from the actual controllers in `infrastructure/controller/` on 2026-10-01.
Where this file and the controllers disagree, the controllers win.

- Base URL (dev, `application-dev.yaml`): `http://localhost:8085`
- Base URL (prod, `application-prod.yaml`): `http://localhost:8080`
- Content-Type: `application/json`
- Auth: `Authorization: Bearer <accessToken>`

## Authentication

Handled by `LoginController`, which maps endpoints individually rather than through a
class-level `@RequestMapping`. Public paths are declared in `SecurityConfig`:

```java
.requestMatchers("/api/auth/login", "/api/auth/register",
                 "/api/auth/refresh",  "/api/auth/logout").permitAll()
.anyRequest().authenticated()
```

`OPTIONS /**` is also permitted for CORS preflight.

---

### POST /api/auth/login

**Auth required:** No

```json
{ "email": "user@example.com", "password": "secret" }
```

`LoginRequest` accepts `username` as a `@JsonAlias` for `email`.

**200 OK**
```json
{ "accessToken": "string", "refreshToken": "string" }
```

**Errors** — handled by the local `@ExceptionHandler`s in `LoginController`, so these return a
**bare JSON string**, not the standard `ErrorResponse` shape:

- `400 Bad Request` — `IllegalArgumentException`
- `409 Conflict` — `IllegalStateException`

---

### POST /api/auth/register

**Auth required:** No

```json
{ "email": "user@example.com", "password": "secret", "name": "User Name" }
```

**201 Created**
```json
{ "accessToken": "string", "refreshToken": "string" }
```

New members are created with tier `FREE`. The refresh token is SHA-256 hashed and stored as
`Member.tokenHash`.

---

### POST /api/auth/refresh

**Auth required:** No

```json
{ "refreshToken": "string" }
```

**200 OK** — `{ "accessToken": "string", "refreshToken": "string" }` (rotated pair)

---

### POST /api/auth/logout

**Auth required:** No

Body optional (`@RequestBody(required = false)`).

**204 No Content**

---

### GET /api/auth/profile

**Auth required:** Yes. Identity comes from `Principal.getName()` (the JWT subject = email).

**200 OK**
```json
{
  "memberId": "uuid", "email": "string", "tier": "FREE",
  "registeredAt": "ISO-8601", "personId": "uuid",
  "firstName": "string", "lastName": "string", "gender": "string",
  "birthDate": "YYYY-MM-DD", "nationality": "string",
  "federationLicense": "string"
}
```

---

### PUT /api/auth/profile

**Auth required:** Yes

```json
{
  "firstName": "string", "lastName": "string", "gender": "string",
  "birthDate": "YYYY-MM-DD", "nationality": "string",
  "federationLicense": "string"
}
```

**200 OK** — same shape as `GET /api/auth/profile`.

> Implemented in the backend but never called by `auth.service.ts`.

---

## Tournaments

`TournamentController`, base path `/api/tournaments`. IDs are **UUIDs**, not longs.

### GET /api/tournaments

**Auth required:** Yes · **200 OK** — `TournamentResponse[]`

### GET /api/tournaments/{id}

**Auth required:** Yes · Path param `id` is a `UUID`.

**200 OK** — `TournamentResponse`
**404 Not Found** — empty body (`ResponseEntity.notFound().build()`)

### POST /api/tournaments

**Auth required:** Yes. The organiser is taken from `Principal.getName()`, not the body.

```json
{
  "formalName": "string",
  "playStartDate": "YYYY-MM-DD", "playEndDate": "YYYY-MM-DD",
  "inscriptionStartDate": "YYYY-MM-DD", "inscriptionEndDate": "YYYY-MM-DD",
  "surfaceCategory": "CLAY | HARD | GRASS | CARPET",
  "maxPlayers": 32,
  "location": "string"
}
```

**201 Created** — `TournamentResponse`. Defaults to `status: DRAFT`.

### POST /api/tournaments/{tournamentId}/events

**Auth required:** Yes

```json
{ "events": [{ "categoryId": 1, "gender": "MALE" }] }
```

Accepted `gender` values: `MALE`, `FEMALE`, `MIXED`, `OPEN`. Events whose `categoryId` already
exists on the tournament are ignored rather than duplicated.

**200 OK** — `TournamentResponse`

### PATCH /api/tournaments/{id}/status

**Auth required:** Yes

```json
{ "status": "OPEN" }
```

**200 OK** — `TournamentResponse`

Valid transitions are enforced by `TournamentStatus.canTransitionTo`:

| From | To |
|---|---|
| `DRAFT` | `OPEN`, `CANCELLED` |
| `OPEN` | `CLOSED`, `CANCELLED` |
| `CLOSED` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `COMPLETED`, `CANCELLED` |
| `COMPLETED` / `CANCELLED` | *(terminal)* |

### TournamentResponse shape

```json
{
  "id": "uuid",
  "formalName": "string",
  "playStartDate": "YYYY-MM-DD", "playEndDate": "YYYY-MM-DD",
  "inscriptionStartDate": "YYYY-MM-DD", "inscriptionEndDate": "YYYY-MM-DD",
  "surfaceCategory": "CLAY",
  "maxPlayers": 32,
  "location": "string",
  "status": "DRAFT | OPEN | CLOSED | IN_PROGRESS | COMPLETED | CANCELLED",
  "providerOrganisationId": { "id": "uuid", "email": "string", "...": "full Member object" },
  "events": [{ "categoryId": 1, "gender": "M" }]
}
```

`providerOrganisationId` is misleadingly named — `TournamentWebMapper` maps it from the domain's
`createdBy` field, so it serialises as a whole `Member` object, not an id.

---

## Planned endpoints

The Angular `data/services/` layer already references these. They are on the roadmap and will be
available once the corresponding controllers are added:

| Endpoint | Called from | Status |
|---|---|---|
| `GET /api/age-categories` | `TournamentService.getEventCatalog()` | planned |
| `POST /api/tournaments/{id}/inscriptions` | `TournamentService.requestInscription()` | planned |
| `GET /members/{email}` | `MemberService` | planned (`MemberController` pending) |

`MemberService` (`register`, `findByEmail`, `findById`) is already implemented; exposing it over
HTTP is the remaining step.

## Not yet implemented (planned)

| Resource | Notes |
|---|---|
| `GET /api/rankings` | Player rankings by category |
| `GET /api/tournaments/{id}/draw` | Draw / bracket (`DrawEntity` exists, no endpoint) |
| `PUT /api/matches/{id}/result` | Record match result |

## Error response format

`GlobalExceptionHandler` (`@RestControllerAdvice`) returns this record for everything except the
`LoginController` local handlers:

```json
{
  "code": "string",
  "message": "string",
  "status": 4,
  "timestamp": "ISO-8601",
  "path": "/api/...",
  "details": {}
}
```

`details` is omitted when null (`@JsonInclude(NON_NULL)`). Codes in use:

| Code | HTTP | Trigger |
|---|---|---|
| from `BaseException.getCode()` | from `BaseException.getHttpStatus()` | `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidArgumentException`, `UnauthorizedException`, `InvalidTokenException`, `ExpiredTokenException` |
| `VALIDATION_ERROR` | 400 | `MethodArgumentNotValidException` (`details` = field → message map) |
| `RESOURCE_NOT_FOUND` | 404 | `NoHandlerFoundException` |
| `INTERNAL_SERVER_ERROR` | 500 | catch-all `Exception` handler |

`AuthService` converts validation and authentication problems into domain exceptions
(`DuplicateResourceException`, `UnauthorizedException`), so those produce meaningful 4xx responses.

## Correlation ID

Planned. The log patterns in both `application-dev.yaml` and `application-prod.yaml` interpolate
`%X{correlationId}`, and a `CorrelationIdFilter` added under `infrastructure/` will populate that
MDC key. Until then the placeholder renders empty.
