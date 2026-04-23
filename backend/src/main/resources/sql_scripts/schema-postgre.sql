-- Ensure UUID generation function
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_gender AS ENUM ('H', 'M');
CREATE TYPE category_genre AS ENUM ('H', 'M', 'X');
CREATE TYPE category_mode AS ENUM ('SINGLE', 'DOUBLES');
CREATE TYPE tournament_state AS ENUM ('SOON', 'INSCRIPTION', 'PLAYING', 'FINISHED');

-- PERSONS (base entity, aligned with international tennis IDs)
CREATE TABLE persons (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tennis_id     VARCHAR(20) UNIQUE,           -- IPIN / ITF global ID
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100),
    nationality   CHAR(3),                      -- ISO 3166-1 alpha-3
    birth_date    DATE,
    gender        VARCHAR(10)                   -- MALE / FEMALE
);

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token_hash    VARCHAR(128),
    tier          VARCHAR(32) DEFAULT 'FREE',
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    person_id     UUID REFERENCES persons(id) ON DELETE SET NULL  -- vínculo opcional
);

CREATE TABLE categories (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(255),
    genre   VARCHAR(1),
    mode    VARCHAR(20)
);

-- TOURNAMENTS
CREATE TABLE tournaments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(200) NOT NULL,
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    inscription_start_date   DATE,
    inscription_end_date     DATE,
    surface                  VARCHAR(20),                -- CLAY / HARD / GRASS / CARPET
    max_players             INTEGER,
    location                VARCHAR(200),
    state                   VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT / OPEN / CLOSED / IN_PROGRESS / COMPLETED / CANCELLED
    created_by              UUID REFERENCES users(id) ON DELETE SET NULL,
    venue                   VARCHAR(200),
    country                 CHAR(3),  --TO DELETE
    category                VARCHAR(50)     --TO DELETE            -- ITF / ATP / WTA / NATIONAL
);

CREATE TABLE tournament_categories (
    tournament_id   UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    category_id     UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (tournament_id, category_id)
);

-- PARTICIPANTS (persons or pairs/teams registered in a tournament)
CREATE TABLE participants (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id    UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    person_id        UUID REFERENCES persons(id),   -- NULL if participant is a pair/team
    participant_type VARCHAR(20) NOT NULL,          -- INDIVIDUAL / PAIR / TEAM
    entry_status     VARCHAR(30),                   -- DIRECT_ACCEPTANCE / WILDCARD / QUALIFIER / LUCKY_LOSER
    seed             INTEGER
);

-- PAIR / TEAM MEMBERS mapping (many-to-many between participants and persons)
CREATE TABLE participant_persons (
    participant_id  UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    person_id       UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    PRIMARY KEY (participant_id, person_id)
);

-- REFERENCIA DE CATEGORÍAS DE EDAD
CREATE TABLE ref_age_category (
    id SERIAL PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    description VARCHAR(15)
);

COMMENT ON COLUMN ref_age_category.category IS 'Nombre de la categoría';
COMMENT ON COLUMN ref_age_category.description IS 'Comentario de la categoría';

-- EVENTS (within a tournament: singles/doubles, age categories, gender)
CREATE TABLE events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id   UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    age_category_id INTEGER REFERENCES ref_age_category(id),
    name            VARCHAR(200),
    discipline      VARCHAR(30),               -- TENNIS / BEACH_TENNIS / WHEELCHAIR
    event_type      VARCHAR(20),               -- SINGLES / DOUBLES / TEAM
    gender          VARCHAR(10),               -- MALE / FEMALE / MIXED / OPEN
    draw_size       INTEGER
);

CREATE TABLE inscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    participant_id  UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    status          VARCHAR(20) DEFAULT 'PENDING',
    payment_status  VARCHAR(20) DEFAULT 'UNPAID',
    registered_at   TIMESTAMPTZ DEFAULT NOW(),
    
    UNIQUE (event_id, participant_id)
);

CREATE INDEX idx_inscriptions_event ON inscriptions (event_id);
CREATE INDEX idx_inscriptions_member ON inscriptions (participant_id);

-- STAGES (phases within an event: qualifying, main draw, etc.)
CREATE TABLE stages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    stage_number    INTEGER NOT NULL,
    stage_type      VARCHAR(30)                 -- QUALIFYING / MAIN / CONSOLATION / PLAYOFF
);

-- DRAWS (structure inside a stage)
CREATE TABLE draws (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stage_id        UUID NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    draw_type       VARCHAR(30) NOT NULL,      -- ELIMINATION / ROUND_ROBIN
    draw_name       VARCHAR(100)
);

-- MATCHUPS (individual matches)
CREATE TABLE matchups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draw_id         UUID NOT NULL REFERENCES draws(id) ON DELETE CASCADE,
    round_number    INTEGER,
    match_number    INTEGER,
    match_format    VARCHAR(50),               -- format string e.g. SET3-S:6/TB7
    status          VARCHAR(20),               -- UPCOMING / IN_PROGRESS / COMPLETED / ABANDONED
    scheduled_at    TIMESTAMPTZ,
    court           VARCHAR(50),
    winner_side     SMALLINT                   -- 1 or 2
);

-- MATCHUP SIDES (side 1 and side 2, referencing participants)
CREATE TABLE matchup_sides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matchup_id      UUID NOT NULL REFERENCES matchups(id) ON DELETE CASCADE,
    side_number     SMALLINT NOT NULL,         -- 1 or 2
    participant_id  UUID REFERENCES participants(id) ON DELETE SET NULL,
    UNIQUE (matchup_id, side_number)
);

-- SETS (per-set results for a matchup)
CREATE TABLE sets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matchup_id      UUID NOT NULL REFERENCES matchups(id) ON DELETE CASCADE,
    set_number      SMALLINT NOT NULL,
    side1_games     SMALLINT,
    side2_games     SMALLINT,
    side1_tiebreak  SMALLINT,
    side2_tiebreak  SMALLINT,
    UNIQUE (matchup_id, set_number)
);

-- RANKINGS (historical ranking snapshots per player)
CREATE TABLE rankings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id       UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    ranking_date    DATE NOT NULL,
    ranking_type    VARCHAR(50),               -- ITF / ATP / WTA / NATIONAL
    rank_position   INTEGER,
    ranking_points  NUMERIC(10,2),
    UNIQUE (person_id, ranking_date, ranking_type)
);
