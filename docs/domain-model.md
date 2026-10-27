# Domain Model

Verified against `domain/models/`, `infrastructure/persistence/entity/`, and
`sql_scripts/schema-postgre.sql` on 2026-10-27.

The relational schema is the most complete description of the model, because it is what
`compose.yaml` executes. Where a concept exists only as a table or only as a JPA entity, this
document says so.

## Status summary

The domain layer covers members, people, tournaments and events. The lower half of the competition
model (stages, draws, matchups, sets) and the registration/ranking tables are currently modelled at
the persistence or SQL layer only.

| Concept | Domain class | JPA entity | SQL table |
|---|---|---|---|
| Member | ✅ `Member` | ✅ `MemberEntity` | `users` |
| Person | ✅ `Person` | ✅ `PersonEntity` | `persons` |
| Tournament | ✅ `Tournament` | ✅ `TournamentEntity` | `tournaments` |
| Tournament period | ✅ `TournamentPeriod` (record) | — | columns on `tournaments` |
| Event | ✅ `Event` | ✅ `EventEntity` | `events` |
| Stage | ❌ | ✅ `StageEntity` | `stages` |
| Draw | ❌ | ✅ `DrawEntity` | `draws` |
| Matchup | ❌ | ✅ `MatchupEntity` | `matchups` |
| Matchup side | ❌ | ✅ `MatchupSideEntity` | `matchup_sides` |
| Match set | ❌ | ✅ `MatchSetEntity` | `sets` |
| Age category ref | ❌ | ✅ `RefAgeCategoryEntity` | `ref_age_category` |
| Category | ❌ | ❌ | `categories` |
| Tournament↔category | ❌ | ❌ | `tournament_categories` |
| Participant | ❌ | ❌ | `participants` |
| Pair/team members | ❌ | ❌ | `participant_persons` |
| Inscription | ❌ | ❌ | `inscriptions` |
| Ranking | ❌ | ❌ | `rankings` |

## Entity map (as built)

```
persons ──1:1?──► users (MemberEntity)
   │                   │
   │                   │ created_by
   │                   ▼
   │              tournaments ──1:n──► events ──1:n──► stages ──1:n──► draws ──1:n──► matchups
   │                                  ▲                  │                                    │
   │                       ref_age_category            │                          ┌─────────┴─────────┐
   │                                  │                  ▼                          ▼                   ▼
   └──1:n──► participants ◄──1:n── inscriptions      draws              matchup_sides           sets

   participants ──m:n──► persons  (participant_persons, for pairs/teams)
   tournaments  ──m:n──► categories (tournament_categories)
   persons ──1:n──► rankings
```

Six tables have **no JPA entity at all**: `categories`, `tournament_categories`, `participants`,
`participant_persons`, `inscriptions`, `rankings`. They are created and seeded by SQL but are not
reachable from Java. Note in particular that `inscriptions` joins **events to participants**, not
members to tournaments — the frontend's `POST /api/tournaments/{id}/inscriptions` assumes the
latter and will not map onto this schema as written.

## Key conventions

- **All primary keys are `UUID`** (`gen_random_uuid()`), except `ref_age_category.id`, which is
  `SERIAL`/`Integer` because `EventEntity` references it as an `Integer` FK.
- Java enums are persisted with `@Enumerated(EnumType.STRING)`. Do not remove it — the columns are
  `VARCHAR`, and JPA's default `ORDINAL` mapping would fail to bind.
- Everything below the aggregate root is a **plain `String`** in the schema, with the allowed values
  documented only in SQL comments (`event_type`, `discipline`, `stage_type`, `draw_type`,
  `matchups.status`, `participants.entry_status`, ...). Those comments are the only spec.
- `schema-postgre.sql` declares four PostgreSQL enum types (`user_gender`, `category_genre`,
  `category_mode`, `tournament_state`) that no column currently references; columns use `VARCHAR`
  instead. `tournament_state` (`SOON`/`INSCRIPTION`/`PLAYING`/`FINISHED`) also predates the current
  `DRAFT`-based lifecycle, so align it before adopting it.

## Domain layer — `domain/models/`

### Tournament

Immutable aggregate root. Built via Lombok `@Builder` with a **custom `build()`** that performs all
validation; `buildInternal()` is the generated method and must not be called directly.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | |
| `name` | `String` | required, non-blank |
| `playPeriod` | `TournamentPeriod` | required |
| `inscriptionPeriod` | `TournamentPeriod` | required |
| `surface` | `Surface` | required |
| `maxPlayers` | `Integer` | required, `> 0` |
| `location` | `String` | required |
| `state` | `TournamentStatus` | defaults to `DRAFT` when null |
| `createdBy` | `Member` | organiser, set by the service from `Principal` |
| `events` | `List<Event>` | `@Singular`; duplicates rejected |

`events` is excluded from `toString()` and `equals()`/`hashCode()`. The builder rejects duplicate
`(categoryId, gender)` pairs; `addEvent(Event)` additionally rejects a repeated `categoryId`, while
`addEvent(List<Event>)` **silently filters out** events whose `categoryId` already exists. That
asymmetry is deliberate but easy to trip over.

### TournamentPeriod

`record TournamentPeriod(LocalDate startDate, LocalDate endDate)` — no validation of its own.

### Member

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | |
| `email` | `String` | login identity, and the JWT subject |
| `username` | `String` | **always null** — `MemberMapper` maps it `ignore = true` |
| `password` | `String` | BCrypt hash, mapped from the `password_hash` column |
| `tokenHash` | `String` | SHA-256 of the refresh token |
| `gender` | `String` | `'H'` or `'M'`; **always null** — also `ignore = true` |
| `tier` | `MemberTier` | |
| `registeredAt` | `LocalDateTime` | `@CreationTimestamp`, `updatable = false` |
| `personId` | `UUID` | optional link to `persons`, `ON DELETE SET NULL` |

Members have no `roles` collection; authorisation is handled at the HTTP layer by `SecurityConfig`,
which authenticates every non-public path and does not apply per-role rules.

### Person

Immutable value object built with Lombok `@Builder(toBuilder = true)`.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | |
| `tennisId` | `String` | ITF/IPIN identifier; also used for the federation licence |
| `firstName` | `String` | |
| `lastName` | `String` | |
| `nationality` | `String` | ISO 3166-1 alpha-3 |
| `birthDate` | `LocalDate` | |
| `gender` | `String` | `MALE`, `FEMALE` or `MIXED` |

A member is linked to a person when the member completes their profile
(`PUT /api/auth/profile`).

### Event

A sub-competition within a tournament, identified by age category and gender.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | |
| `tournamentId` | `UUID` | owning tournament |
| `categoryId` | `Integer` | FK to `ref_age_category` |
| `gender` | `String` | `MALE`, `FEMALE`, `MIXED` or `OPEN` |

See the `Tournament` section for the uniqueness rules applied when events are added.

## Enums — `domain/models/enums/`

| Enum | Values |
|---|---|
| `Surface` | `CLAY`, `HARD`, `GRASS`, `CARPET` |
| `TournamentStatus` | `DRAFT`, `OPEN`, `CLOSED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `MemberTier` | `FREE`, `INTERMEDIATE`, `ADVANCED` |
| `AgeCategoryEnum` | **no constants** — see below |

`AgeCategoryEnum` is declared with `id` and `category` fields but no enumerators: the values are
loaded from `ref_age_category` at runtime, so the type acts as a DTO shape rather than a fixed enum.
Use the repository data, not enum constants, when working with age categories.

`TournamentStatus.canTransitionTo(next)` is the authoritative state machine:

| From | Allowed next |
|---|---|
| `DRAFT` | `OPEN`, `CANCELLED` |
| `OPEN` | `CLOSED`, `CANCELLED` |
| `CLOSED` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `COMPLETED`, `CANCELLED` |
| `COMPLETED`, `CANCELLED` | terminal |

## Persistence notes

- `MemberEntity` maps to the `users` table (not `members`); keep that in mind when reading raw SQL.
- `TournamentEntity` stores the domain's `name` in a field called `formalName`, and `playPeriod` as
  flat `playStartDate`/`playEndDate` columns. `TournamentWebMapper` and `TournamentEntityMapper`
  bridge the flat/aggregate split. Its `state` column is mapped to a field named `status`, so the
  entity and the domain use different property names (`Tournament.state` vs
  `TournamentEntity.status`).
- `PersonEntity.gender` is free-text `VARCHAR(10)` documented as `MALE`/`FEMALE`, whereas the older
  `MemberEntity` gender convention used `'H'`/`'M'`. Prefer the `Person` convention for new work.
- `tournaments` also carries `venue`, `country` and `category`, which are marked `--TO DELETE` in the
  schema and have no corresponding field on `TournamentEntity`.
- The string-typed columns listed above are plain `String` properties on their entities
  (`EventEntity.eventType`, `StageEntity.stageType`, `DrawEntity.drawType`, ...).

## Seed data

`sql_scripts/data-postgre.sql` populates `persons`, `users`, `ref_age_category`, `tournaments`,
`events`, `stages`, `draws`, `matchups`, `matchup_sides`, `participants`. It does **not** seed
`inscriptions`, `rankings`, `categories`, or `tournament_categories`.

The scripts are mounted into `docker-entrypoint-initdb.d`, so they only run against a fresh volume.
See `AGENTS.md` for the reset procedure.

## Examples

`backend/src/main/resources/model_examples/` contains `tournament.json`. Add further examples there
as the remaining resources gain a stable JSON shape.
