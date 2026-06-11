# API Specification

Base URL:

- Docker Compose / `BACKEND_PORT=8080`: `http://localhost:8080`
- Local H2 profile (`devH2`, default `BACKEND_PORT` unset): `http://localhost:8080`
- Local PostgreSQL `dev` profile (`BACKEND_PORT` unset): `http://localhost:8085`

Frontend API configuration normally points to the `/api` namespace, for example `http://localhost:8080/api`.

Authentication: `Authorization: Bearer <JWT token>` is required except where marked public.

Content-Type: `application/json`

---

## Public Endpoints

The following endpoints are public in `SecurityConfig`:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/confirm-email`
- `POST /api/auth/resend-confirmation`
- `GET /api/calendar/tournaments`
- `GET /api/tournaments/{tournamentId}/inscriptions`
- `GET /api/rankings/**`
- `GET /api/age-categories`

All other endpoints require a valid JWT.

---

## Authentication

### POST /api/auth/login

Authenticate a member and receive access and refresh tokens.

Authentication required: no

Request:

```json
{
  "email": "user@example.com",
  "password": "string"
}
```

`username` is accepted as an alias for `email` for compatibility.

Response: `200 OK`

```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

### POST /api/auth/register

Register a new member.

Authentication required: no

Request:

```json
{
  "email": "user@example.com",
  "password": "string",
  "name": "string"
}
```

Response: `201 Created`

```json
{
  "emailVerificationRequired": true,
  "message": "string"
}
```

### GET /api/auth/confirm-email

Confirm a registered email address.

Authentication required: no

Query parameters:

- `token`: email confirmation token

Response: `200 OK`

### POST /api/auth/resend-confirmation

Request another confirmation email.

Authentication required: no

Request uses the registration DTO; only `email` is used by the current controller.

### POST /api/auth/refresh

Refresh access and refresh tokens.

Authentication required: no

Request:

```json
{
  "refreshToken": "string"
}
```

Response: `200 OK`

```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

### POST /api/auth/logout

Invalidate a refresh token.

Authentication required: no

Request body is optional:

```json
{
  "refreshToken": "string"
}
```

Response: `204 No Content`

### GET /api/auth/profile

Return the current user's profile.

Authentication required: yes

### PUT /api/auth/profile

Complete or update the current user's profile.

Authentication required: yes

Request fields include `firstName`, `lastName`, `gender`, `birthDate`, `nationality`, and `federationLicense`.

---

## Tournaments

### GET /api/tournaments

Retrieve tournament summaries.

Authentication required: yes

Response: `200 OK`

### GET /api/tournaments/{id}

Retrieve a tournament by UUID, including nested events, stages, draws, matches, and courts where available.

Authentication required: yes

Response: `200 OK`

### POST /api/tournaments

Create a new tournament.

Authentication required: yes

Request:

```json
{
  "formalName": "string",
  "playStartDate": "YYYY-MM-DD",
  "playEndDate": "YYYY-MM-DD",
  "tournamentStartTime": "HH:mm:ss",
  "inscriptionStartDate": "YYYY-MM-DD",
  "inscriptionEndDate": "YYYY-MM-DD",
  "surfaceCategory": "CLAY | HARD | GRASS | CARPET",
  "maxPlayers": 32,
  "location": "string",
  "courtCount": 4
}
```

Response: `201 Created`

### PATCH /api/tournaments/{id}/status

Update tournament status.

Authentication required: yes

Request:

```json
{
  "status": "DRAFT | PUBLISHED | IN_PROGRESS | FINISHED | CANCELLED"
}
```

### POST /api/tournaments/{tournamentId}/events

Replace the tournament events definition.

Authentication required: yes

### DELETE /api/tournaments/{tournamentId}/events/{eventId}

Remove one event from a tournament.

Authentication required: yes

### POST /api/tournaments/{tournamentId}/events/{eventId}/generate-draws

Generate draws and matches for an event.

Authentication required: yes

### POST /api/tournaments/{tournamentId}/events/{eventId}/inscriptions

Register the authenticated member in an event.

Authentication required: yes

### POST /api/tournaments/{tournamentId}/events/{eventId}/manual-inscriptions

Register a player manually in an event.

Authentication required: yes

### GET /api/tournaments/{tournamentId}/events/{eventId}/inscriptions

List inscriptions for a tournament event.

Authentication required: yes

### GET /api/tournaments/{tournamentId}/inscriptions

List tournament inscriptions. This endpoint is public.

Optional query parameters:

- `eventId`

### GET /api/tournaments/{tournamentId}/courts

List courts for a tournament.

Authentication required: yes

### POST /api/tournaments/{tournamentId}/courts

Create a court.

Authentication required: yes

### PATCH /api/tournaments/{tournamentId}/courts/{courtId}

Rename a court.

Authentication required: yes

### DELETE /api/tournaments/{tournamentId}/courts/{courtId}

Delete a court.

Authentication required: yes

### POST /api/tournaments/{tournamentId}/matches/{matchId}/result

Record a match result.

Authentication required: yes

Request:

```json
{
  "winnerId": "uuid",
  "scoreString": "6-4 6-3"
}
```

### PATCH /api/tournaments/{tournamentId}/matches/{matchId}/schedule

Schedule a match.

Authentication required: yes

Request fields include `courtId`, `scheduledAt`, and `scheduleTimeType`.

---

## Calendar

### GET /api/calendar/tournaments

List published tournaments for the public calendar.

Authentication required: no

Optional query parameters:

- `from`: ISO date
- `to`: ISO date
- `surface`
- `location`

### GET /api/calendar/my-matches

List scheduled matches for the authenticated player.

Authentication required: yes

Optional query parameters:

- `from`: ISO date
- `to`: ISO date

---

## Rankings and Search

### GET /api/rankings/professionals

List professional ranking entries.

Authentication required: no

Optional query parameters:

- `gender`
- `category`

### GET /api/rankings/tournaments

List tournaments available for rankings.

Authentication required: no

### GET /api/rankings/tournaments/{tournamentId}

List ranking entries for a tournament.

Authentication required: no

Optional query parameters:

- `gender`
- `categoryId`

### GET /api/pro-players

Search professional players.

Authentication required: yes

Optional query parameters:

- `query`

### GET /api/persons

Search people.

Authentication required: yes

Optional query parameters:

- `query`

### GET /api/age-categories

List age categories.

Authentication required: no

---

## Members

### POST /api/members

Register a member through `MemberController`.

Authentication required: yes

### GET /api/members/{email}

Find a member by email.

Authentication required: yes

---

## Matches

### GET /api/matches/tournament/{tournamentId}

List matches for a tournament.

Authentication required: yes

### PUT /api/matches/{id}

Update a match.

Authentication required: yes

The active frontend result and scheduling flows use the nested tournament match endpoints:

- `POST /api/tournaments/{tournamentId}/matches/{matchId}/result`
- `PATCH /api/tournaments/{tournamentId}/matches/{matchId}/schedule`

---

## Error Response Format

Errors handled by `GlobalExceptionHandler` follow this shape:

```json
{
  "code": "string",
  "message": "string",
  "status": 400,
  "timestamp": "ISO-8601",
  "path": "/api/...",
  "details": {}
}
```

`details` is omitted when there are no additional details.

---

## Correlation ID

Every request is tagged with a `correlationId` injected by `CorrelationIdFilter` into the MDC.
Log entries include the pattern: `%5p [req-id: %X{correlationId}]`.
