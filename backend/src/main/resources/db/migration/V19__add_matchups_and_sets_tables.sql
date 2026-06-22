CREATE TABLE IF NOT EXISTS matchups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draw_id UUID NOT NULL REFERENCES draws(id) ON DELETE CASCADE,
    round_number INTEGER,
    match_number INTEGER,
    match_format VARCHAR(30),
    status VARCHAR(30),
    scheduled_at TIMESTAMPTZ,
    court VARCHAR(50),
    winner_side SMALLINT
);

CREATE TABLE IF NOT EXISTS matchup_sides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matchup_id UUID NOT NULL REFERENCES matchups(id) ON DELETE CASCADE,
    side_number SMALLINT NOT NULL,
    participant_id UUID,
    UNIQUE (matchup_id, side_number)
);

CREATE TABLE IF NOT EXISTS sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matchup_id UUID NOT NULL REFERENCES matchups(id) ON DELETE CASCADE,
    set_number SMALLINT NOT NULL,
    side1_games INTEGER,
    side2_games INTEGER,
    side1_tiebreak INTEGER,
    side2_tiebreak INTEGER,
    UNIQUE (matchup_id, set_number)
);
