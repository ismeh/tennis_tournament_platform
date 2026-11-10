# API Specification

Base URL:

- Docker Compose / `BACKEND_PORT=8080`: `http://localhost:8080`
- Local H2 profile (`devH2`, default `BACKEND_PORT` unset): `http://localhost:8080`
- Local PostgreSQL `dev` profile (`BACKEND_PORT` unset): `http://localhost:8080`

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
- `GET /api/tournaments/{tournamentId}/updates`
- `GET /api/tournaments/{tournamentId}/inscriptions`
- `GET /api/rankings/**`
- `GET /api/age-categories`
- `GET /api/clubs`
- `GET /api/clubs/permissions`
- `GET /api/clubs/public/**`
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

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

### GET /api/tournaments/{tournamentId}/updates

Subscribe to lightweight Server-Sent Events for tournament bracket changes.

Authentication required: no

Response content type: `text/event-stream`

Emitted event names:

- `connected`
- `match-result-updated`
- `match-schedule-updated`
- `heartbeat`

Update event payload:

```json
{
  "type": "MATCH_RESULT_UPDATED",
  "tournamentId": "uuid",
  "matchId": "uuid",
  "occurredAt": "2026-06-11T10:00:00"
}
```

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
  "courtCount": 4,
  "clubId": "uuid or null"
}
```

`clubId` is optional. When present, the authenticated user must belong to the
club and hold the `MANAGE_TOURNAMENTS` permission.

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

### GET /api/tournaments/{tournamentId}/export/xlsx

Export a complete tournament snapshot as an Excel workbook
(`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).

Authentication required: yes

The workbook contains four sheets:

- `Resumen`: tournament information, format, and aggregate counters.
- `Inscritos`: registered players with event, category, points, seed, club, and status.
- `Partidos`: complete match list with schedule, court, status, result, and winner. Matches whose participants are still undecided contain placeholders such as `Ganador de P-003`.
- `Pistas`: configured courts and their activity state.

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

## Clubs

Clubs are organizational tenants with an optional public page, member roles,
granular permissions, and an aggregated internal ranking.

### Public endpoints

Authentication required: no

- `GET /api/clubs` — lightweight club search used by autocomplete (`?q=`).
- `GET /api/clubs/public` — public club directory (`?q=` optional).
- `GET /api/clubs/public/{slug}` — public club page.
- `GET /api/clubs/public/{slug}/rankings` — club ranking (`?gender=MALE|FEMALE|MIXED` optional).
- `GET /api/clubs/public/{slug}/tournaments` — tournaments associated with the club.
- `GET /api/clubs/permissions` — role/permission catalog.

Public pages and rankings are only exposed when the club has
`publicPageEnabled` set to `true`.

### Authenticated management endpoints

Authentication required: yes

- `POST /api/clubs` — create a club; the requester becomes `OWNER`.
- `GET /api/clubs/mine` — clubs the requester belongs to.
- `GET /api/clubs/{clubId}/management` — club page, requester membership, and visible members.
- `GET /api/clubs/{clubId}/rankings` — internal ranking for members (`?gender=` optional).
- `GET /api/clubs/{clubId}/tournaments` — tournaments associated with the club for members.
- `PUT /api/clubs/{clubId}/page` — update branding, contact, social links, and visibility.
- `GET /api/clubs/{clubId}/members` — list members (`VIEW_MEMBERS`).
- `POST /api/clubs/{clubId}/members` — add a member by email (`MANAGE_MEMBERS`).
- `PUT /api/clubs/{clubId}/members/{membershipId}/role` — change a role (`MANAGE_PERMISSIONS`).
- `PUT /api/clubs/{clubId}/members/{membershipId}/permissions` — set custom permission grants (`MANAGE_PERMISSIONS`).
- `DELETE /api/clubs/{clubId}/members/{membershipId}` — remove a member (`MANAGE_MEMBERS`).

### Roles and permissions

Roles: `OWNER`, `ADMIN`, `COACH`, `MEMBER`.

Permissions: `EDIT_CLUB_PAGE`, `VIEW_MEMBERS`, `MANAGE_MEMBERS`,
`MANAGE_PERMISSIONS`, `VIEW_RANKINGS`, `MANAGE_TOURNAMENTS`.

Every role has default permissions. A membership can additionally receive
custom grants, and its effective permissions are the union of both sets.
`OWNER` always has every permission and cannot be removed or reassigned.

### Club ranking

The ranking aggregates completed singles matches for persons whose `club_id`
matches the club. Entries are ordered by accumulated points and victories;
players with the same points share a position.

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

## In-app Notifications and Web Push

All endpoints require authentication except the public key.

| Method | Path | Description |
| ------ | ---- | ----------- |
| GET | `/api/notifications` | List the current user's in-app notifications. |
| GET | `/api/notifications/unread-count` | Unread counter for the header bell. |
| PATCH | `/api/notifications/{id}/read` | Mark one notification as read. |
| PATCH | `/api/notifications/read-all` | Mark all notifications as read. |
| GET | `/api/notifications/preferences` | Current channel: `APP`, `EMAIL`, `BOTH` or `NONE`. |
| PUT | `/api/notifications/preferences` | Update the channel (`{ "channel": "APP" }`). |
| GET | `/api/push/public-key` | Public VAPID key and whether push is enabled. No authentication required. |
| POST | `/api/push/subscriptions` | Register or refresh this browser subscription. |
| DELETE | `/api/push/subscriptions?endpoint=...` | Remove the subscription for the current user. |
| POST | `/api/push/test` | Send a test notification to the user's subscribed devices. |

Push is attempted only when the notification channel includes the app (`APP` or `BOTH`). Delivery
is best-effort and never blocks or fails the in-app notification.

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
