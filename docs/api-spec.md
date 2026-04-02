# API Specification

Base URL (dev): `http://localhost:8085`
Base URL (prod): `http://localhost:8080`

Authentication: `Authorization: Bearer <JWT token>` (required on all endpoints except `/api/login`)

Content-Type: `application/json`

---

## Authentication

### POST /api/login

Authenticate a member and receive a JWT token.

**Authentication required:** No

**Request**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response — 201 Created**
```json
{
  "token": "string"
}
```

**Error Responses**
- `401 Unauthorized` — invalid credentials

---

## Tournaments

### GET /api/tournaments

Retrieve all tournaments.

**Authentication required:** Yes

**Response — 200 OK**
```json
[
  {
    "id": 1,
    "name": "string",
    "status": "PLANNED | OPEN | IN_PROGRESS | FINISHED | CANCELLED"
  }
]
```

---

### GET /api/tournaments/{id}

Retrieve a tournament by ID.

**Authentication required:** Yes

**Path Parameters**
- `id` — tournament identifier (long)

**Response — 200 OK**
```json
{
  "id": 1,
  "name": "string",
  "status": "string"
}
```

**Error Responses**
- `404 Not Found` — tournament does not exist

---

### POST /api/tournaments

Create a new tournament.

**Authentication required:** Yes

**Request**
```json
{
  "formalName": "string",
  "playStartDate": "YYYY-MM-DD",
  "playEndDate": "YYYY-MM-DD",
  "inscriptionStartDate": "YYYY-MM-DD",
  "inscriptionEndDate": "YYYY-MM-DD",
  "surfaceCategory": "CLAY | HARD | GRASS | CARPET",
  "maxPlayers": 32,
  "location": "string"
}
```

**Response — 200 OK**
```json
{
  "id": "uuid",
  "formalName": "string",
  "playStartDate": "YYYY-MM-DD",
  "playEndDate": "YYYY-MM-DD",
  "inscriptionStartDate": "YYYY-MM-DD",
  "inscriptionEndDate": "YYYY-MM-DD",
  "surfaceCategory": "CLAY",
  "maxPlayers": 32,
  "location": "string",
  "status": "DRAFT",
  "providerOrganisationId": "uuid"
}
```

---

## Members

### GET /members

Retrieve all members.

**Authentication required:** Yes

**Response — 200 OK**
```json
[
  {
    "id": 1,
    "username": "string",
    "roles": ["string"]
  }
]
```

> Member endpoints defined in `MemberController.java`. Full schema in `MemberResponse.java`.

---

## Matches

> Match endpoints defined in `MatchController.java`. Full schema in `MatchResponse.java`.

### GET /matches

Retrieve all matches.

**Authentication required:** Yes

---

## Not Yet Implemented (Planned)

| Resource | Notes |
|----------|-------|
| `POST /members` | Member registration |
| `GET /rankings` | Player rankings by category |
| `POST /tournaments/{id}/inscriptions` | Member entry to a tournament |
| `GET /tournaments/{id}/draw` | Tournament draw / bracket |
| `PUT /matches/{id}/result` | Record match result |

---

## Error Response Format

All errors follow this shape:

```json
{
  "timestamp": "ISO-8601",
  "status": 4xx | 5xx,
  "error": "string",
  "message": "string",
  "path": "/api/..."
}
```

> Standard Spring Boot error response. Custom exception handlers not yet implemented.

---

## Correlation ID

Every request is tagged with a `correlationId` injected by `CorrelationIdFilter` into the MDC.
Log entries include the pattern: `%5p [req-id: %X{correlationId}]`.
