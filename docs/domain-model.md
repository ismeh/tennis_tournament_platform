# Domain Model

Overview of the core domain entities, their relationships, and key enumerations.

---

## Entity Map

```
Person ──────────────────► Member
                            │
                  ┌─────────┴──────────┐
                  │                    │
              Inscription           (organizer role)
                  │                    │
                  ▼                    ▼
             Tournament ◄──────── Category
                  │
        ┌─────────┴──────────┐
        │                    │
       Event               Ranking
        │
       Stage
        │
       Draw
        │
      Matchup ──► MatchupSide
        │
      MatchSet
```

---

## Entities

### Person

Represents a physical individual.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `firstName` | String | |
| `lastName` | String | |
| `birthDate` | LocalDate | |
| `gender` | Gender enum | `MALE`, `FEMALE`, `OTHER` |

---

### Member

A platform user. Extends `Person` data via association.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `username` | String | Unique login name |
| `password` | String | BCrypt-hashed |
| `roles` | List\<Role\> | `PLAYER`, `ORGANIZER`, `ADMIN` |
| `tier` | UserTier enum | `FREE`, `PRO` |

---

### Tournament

Main aggregate root for a competition.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `name` | String | |
| `surface` | Surface enum | `CLAY`, `HARD`, `GRASS`, `INDOOR` |
| `category` | TournamentCategory enum | See below |
| `discipline` | Discipline enum | `SINGLES`, `DOUBLES`, `MIXED_DOUBLES` |
| `startDate` | LocalDate | |
| `endDate` | LocalDate | |
| `maxParticipants` | Integer | |
| `status` | TournamentStatus enum | See below |
| `drawType` | DrawType enum | `SINGLE_ELIMINATION`, `ROUND_ROBIN` |

---

### Inscription

A member's entry into a tournament.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `member` | Member | |
| `tournament` | Tournament | |
| `inscriptionDate` | LocalDate | |
| `entryStatus` | EntryStatus enum | `PENDING`, `CONFIRMED`, `REJECTED`, `WAITLISTED` |

---

### Event

A sub-competition within a tournament (e.g., Men's U18 Singles).

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `tournament` | Tournament | |
| `type` | EventType enum | `SINGLES`, `DOUBLES`, etc. |
| `ageCategory` | AgeCategory enum | |
| `gender` | Gender enum | |

---

### Stage

A phase within an event (e.g., Round of 16, Semi-final, Final).

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `event` | Event | |
| `stageType` | StageType enum | `ROUND_ROBIN`, `SINGLE_ELIMINATION`, `FINAL`, etc. |
| `order` | Integer | Execution order within the event |

---

### Draw

The bracket or group table for a stage.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `stage` | Stage | |
| `type` | DrawType enum | |

---

### Matchup

A single match between two sides.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `draw` | Draw | |
| `side1` | MatchupSide | |
| `side2` | MatchupSide | |
| `status` | MatchupStatus enum | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `WALKOVER`, `CANCELLED` |
| `sets` | List\<MatchSet\> | |

---

### MatchupSide

One participant side in a matchup (single player or doubles pair).

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `participants` | List\<Participant\> | One for singles, two for doubles |
| `setsWon` | Integer | Computed |

---

### Participant

A player or pair registered as a competitor within a draw.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `member` | Member | |
| `event` | Event | |
| `type` | ParticipantType enum | `SINGLE`, `PAIR` |

---

### MatchSet

One set within a matchup.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `matchup` | Matchup | |
| `setNumber` | Integer | |
| `side1Games` | Integer | |
| `side2Games` | Integer | |

---

### Ranking

A computed standing of a participant in an event.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK |
| `participant` | Participant | |
| `event` | Event | |
| `points` | Integer | |
| `position` | Integer | |
| `type` | RankingType enum | `SINGLES`, `DOUBLES` |

---

## Key Enumerations

| Enum | Values |
|------|--------|
| `TournamentStatus` | `PLANNED`, `OPEN`, `IN_PROGRESS`, `FINISHED`, `CANCELLED` |
| `TournamentCategory` | (see `TournamentCategory.java`) |
| `MatchupStatus` | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `WALKOVER`, `CANCELLED` |
| `EntryStatus` | `PENDING`, `CONFIRMED`, `REJECTED`, `WAITLISTED` |
| `StageType` | `ROUND_ROBIN`, `SINGLE_ELIMINATION`, `DOUBLE_ELIMINATION`, `FINAL`, `SEMIFINAL`, `QUARTERFINAL` |
| `DrawType` | `SINGLE_ELIMINATION`, `ROUND_ROBIN` |
| `Discipline` | `SINGLES`, `DOUBLES`, `MIXED_DOUBLES` |
| `Surface` | `CLAY`, `HARD`, `GRASS`, `INDOOR` |
| `Gender` | `MALE`, `FEMALE`, `OTHER` |
| `UserTier` | `FREE`, `PRO` |
| `ParticipantType` | `SINGLE`, `PAIR` |
| `RankingType` | `SINGLES`, `DOUBLES` |
| `AgeCategory` | (see `AgeCategory.java`) |
| `EventType` | (see `EventType.java`) |

---

## Package Locations

Domain entities (active package): `com.tfm.tennis_platform.domain.models`
Enums (shared with experimental package): `com.tennis.domain.model.enums`
JPA entities: `com.tfm.tennis_platform.infrastructure.persistence.entity`

> Note: enums currently live in `com.tennis.domain.model.enums`. Migration to `com.tfm.tennis_platform.domain.model.enums` is pending.

---

## Model Examples

JSON examples of domain objects are available in:
`backend/src/main/resources/model_examples/`

Files: `tournament.json`, `event.json`, `matchup.json`, `participant.json`, `person.json`, `ranking.json`, `stage.json`, `draw.json`
