# Domain Model

Overview of the current active domain under `com.tfm.tennis_platform.domain.models`.

---

## Aggregate Map

```text
Tournament
  ├─ TournamentPeriod: play period
  ├─ TournamentPeriod: inscription period
  ├─ Court[]
  └─ Event[]
       ├─ Category
       ├─ Stage[]
       │    └─ Draw[]
       │         └─ Match[]
       └─ Inscription[]
            └─ Participant
                 └─ Person[] or ProPlayer

Member
  ├─ Role[]
  └─ Person profile

Club
  ├─ ClubMembership[]
  │    ├─ role: ClubRole
  │    └─ customPermissions: ClubPermission[]
  └─ ClubRankingEntry[] (read model)

Ranking
  ├─ ProfessionalRankingEntry
  └─ TournamentRankingEntry
```

---

## Core Models

### Tournament

Main aggregate root for a competition.

Important fields:

- `id`: UUID
- `name`
- `playPeriod`: `TournamentPeriod`
- `inscriptionPeriod`: `TournamentPeriod`
- `surface`: `Surface`
- `maxPlayers`
- `location`
- `status`: `TournamentStatus`
- `providerOrganisationId`
- `clubId`: optional organizing club
- `events`
- `courts`

### Event

Sub-competition inside a tournament, usually defined by category and gender.

Important fields:

- `id`: UUID
- `tournamentId`
- `category`
- `gender`
- `stages`
- `inscriptions`

### Stage

Phase inside an event.

Important fields:

- `id`: UUID
- `eventId`
- `stageNumber`
- `stageType`: `StageType`
- `draws`

### Draw

Bracket or group generated for a stage.

Important fields:

- `id`: UUID
- `stageId`
- `drawType`: `DrawType`
- `matches`

### Match

Scheduled or completed match inside a draw.

Important fields:

- `id`: UUID
- `drawId`
- `firstInscription`
- `secondInscription`
- `winner`
- `roundNumber`
- `scheduledAt`
- `court`
- `result`
- `nextMatch`
- `loserNextMatch`

### Inscription

Entry of a participant into an event.

Important fields:

- `id`: UUID
- `eventId`
- `participant`
- `status`: `EntryStatus`
- `paymentStatus`
- `registeredAt`

### Participant

Competitor unit for singles or doubles.

Important fields:

- `id`: UUID
- `participantType`: `ParticipantType`
- `participantSource`: `ParticipantSource`
- `members`
- `proPlayer`

### Member

Application user account.

Important fields:

- `id`: UUID
- `email`
- `password`
- `tier`: `MemberTier`
- `roles`
- `person`
- `registeredAt`
- `emailVerified`

### Person

Physical player/person profile.

Important fields:

- `id`: UUID
- `tennisId`
- `firstName`
- `lastName`
- `nationality`
- `birthDate`
- `gender`

### ProPlayer

Imported professional/federated ranking player.

Important fields:

- `id`: Long
- `license`
- `fullName`
- `firstName`
- `lastName`
- `rankingPosition`
- `ageCategory`
- `clubName`
- `birthDate`
- `gender`
- `points`

### Court

Court assigned to a tournament.

Important fields:

- `id`: UUID
- `tournamentId`
- `name`

### Club

Organizational tenant with an optional public page and internal management.

Important fields:

- `id`: UUID
- `name`
- `slug`: unique public URL segment
- `tagline`, `description`
- `logoUrl`, `bannerUrl`
- `primaryColor`, `secondaryColor`
- `city`, `country`
- `contactEmail`, `phone`, `website`
- `instagramUrl`, `facebookUrl`, `xUrl`
- `foundedYear`
- `publicPageEnabled`
- `createdBy`: member UUID

### ClubMembership

Association between a club and a member account.

Important fields:

- `id`: UUID
- `clubId`
- `memberId`
- `role`: `ClubRole`
- `customPermissions`: additional `ClubPermission` grants
- `joinedAt`

Effective permissions are the union of `role.defaultPermissions()` and
`customPermissions`. Only one `OWNER` membership exists per club.

---

## Enumerations

Active enum package: `com.tfm.tennis_platform.domain.models.enums`.

- `AgeCategoryEnum`
- `ClubPermission`
- `ClubRole`
- `DrawType`
- `EntryStatus`
- `MemberTier`
- `ParticipantSource`
- `ParticipantType`
- `ScheduleTimeType`
- `StageType`
- `Surface`
- `TournamentStatus`

---

## Query View Models

The domain also contains read-oriented models for API responses and ranking/calendar projections:

- `TournamentSummary`
- `club/ClubMemberView`
- `club/ClubMembershipSummary`
- `club/ClubRankingEntry`
- `calendar/TournamentCalendarItem`
- `calendar/PlayerMatchCalendarItem`
- `ranking/ProfessionalRankingEntry`
- `ranking/TournamentRankingEntry`
- `inscription/TournamentInscriptionsView`
- `inscription/TournamentInscriptionPlayerView`
- `inscription/TournamentInscriptionEventView`
- `inscription/TournamentInscriptionGenderCount`
- `inscription/TournamentInscriptionCategoryCount`

---

## Architecture Rule

Controllers must only handle request/response DTOs. Application services work with domain models and domain ports. Persistence adapters map JPA entities to domain models before returning data to the application layer.
