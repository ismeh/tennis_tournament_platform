CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_gender') THEN
        CREATE TYPE user_gender AS ENUM ('H', 'M');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'category_genre') THEN
        CREATE TYPE category_genre AS ENUM ('H', 'M', 'X');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'category_mode') THEN
        CREATE TYPE category_mode AS ENUM ('SINGLE', 'DOUBLES');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tournament_state') THEN
        CREATE TYPE tournament_state AS ENUM ('SOON', 'INSCRIPTION', 'PLAYING', 'FINISHED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tennis_id VARCHAR(20) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    nationality CHAR(3),
    birth_date DATE,
    gender VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token_hash VARCHAR(128),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_confirmation_token_hash VARCHAR(128),
    email_confirmation_expires_at TIMESTAMPTZ,
    tier VARCHAR(32) DEFAULT 'FREE',
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    person_id UUID REFERENCES persons(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255),
    genre VARCHAR(1),
    mode VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS tournaments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    inscription_start_date DATE,
    inscription_end_date DATE,
    surface VARCHAR(20),
    max_players INTEGER,
    location VARCHAR(200),
    state VARCHAR(20) DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 0,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    venue VARCHAR(200),
    country CHAR(3),
    category VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS tournament_categories (
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (tournament_id, category_id)
);

CREATE TABLE IF NOT EXISTS participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    person_id UUID REFERENCES persons(id),
    participant_source VARCHAR(30),
    display_first_name VARCHAR(100),
    display_last_name VARCHAR(100),
    display_gender VARCHAR(20),
    display_birth_date DATE,
    display_nationality VARCHAR(50),
    display_tennis_id VARCHAR(50),
    participant_type VARCHAR(20) NOT NULL,
    entry_status VARCHAR(30),
    seed INTEGER
);

CREATE TABLE IF NOT EXISTS participant_persons (
    participant_id UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    PRIMARY KEY (participant_id, person_id)
);

CREATE TABLE IF NOT EXISTS ref_age_category (
    id SERIAL PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    description VARCHAR(15)
);

CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    age_category_id INTEGER REFERENCES ref_age_category(id),
    name VARCHAR(200),
    discipline VARCHAR(30),
    event_type VARCHAR(20),
    gender VARCHAR(10),
    draw_size INTEGER
);

CREATE TABLE IF NOT EXISTS inscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING',
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (event_id, participant_id)
);

CREATE INDEX IF NOT EXISTS idx_inscriptions_event ON inscriptions (event_id);
CREATE INDEX IF NOT EXISTS idx_inscriptions_member ON inscriptions (participant_id);

CREATE TABLE IF NOT EXISTS stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    stage_order INTEGER NOT NULL,
    stage_type VARCHAR(30),
    description VARCHAR
);

CREATE TABLE IF NOT EXISTS draws (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stage_id UUID NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    draw_type VARCHAR(30) NOT NULL,
    label VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draw_id UUID NOT NULL REFERENCES draws(id) ON DELETE CASCADE,
    first_inscription_id UUID REFERENCES inscriptions(id) ON DELETE SET NULL,
    second_inscription_id UUID REFERENCES inscriptions(id) ON DELETE SET NULL,
    winner_id UUID REFERENCES inscriptions(id) ON DELETE SET NULL,
    round_number INTEGER,
    next_match_id UUID REFERENCES matches(id) ON DELETE SET NULL,
    scheduled_at TIMESTAMP,
    court VARCHAR(50),
    result VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rankings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    ranking_date DATE NOT NULL,
    ranking_type VARCHAR(50),
    rank_position INTEGER,
    ranking_points NUMERIC(10,2),
    UNIQUE (person_id, ranking_date, ranking_type)
);
