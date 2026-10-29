-- Add sets configuration and decisive tiebreak options to tournaments
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS sets_per_match INTEGER DEFAULT 3;
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS decisive_tiebreak_points INTEGER DEFAULT 7;

-- Create match sets table
CREATE TABLE IF NOT EXISTS match_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    set_number SMALLINT NOT NULL,
    first_player_games INTEGER NOT NULL DEFAULT 0,
    second_player_games INTEGER NOT NULL DEFAULT 0,
    first_player_tiebreak INTEGER,
    second_player_tiebreak INTEGER,
    UNIQUE (match_id, set_number)
);

-- Ensure notes and status exist on matches table
ALTER TABLE matches ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS status VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_match_sets_match ON match_sets(match_id);

-- Migrate old matches: assign 6-0 6-0 6-0 to matches with winners
-- Set 1
INSERT INTO match_sets (id, match_id, set_number, first_player_games, second_player_games)
SELECT gen_random_uuid(), id, 1, 
       CASE WHEN winner_id = first_inscription_id THEN 6 ELSE 0 END,
       CASE WHEN winner_id = second_inscription_id THEN 6 ELSE 0 END
FROM matches
WHERE winner_id IS NOT NULL AND first_inscription_id IS NOT NULL AND second_inscription_id IS NOT NULL;

-- Set 2
INSERT INTO match_sets (id, match_id, set_number, first_player_games, second_player_games)
SELECT gen_random_uuid(), id, 2, 
       CASE WHEN winner_id = first_inscription_id THEN 6 ELSE 0 END,
       CASE WHEN winner_id = second_inscription_id THEN 6 ELSE 0 END
FROM matches
WHERE winner_id IS NOT NULL AND first_inscription_id IS NOT NULL AND second_inscription_id IS NOT NULL;

-- Set 3
INSERT INTO match_sets (id, match_id, set_number, first_player_games, second_player_games)
SELECT gen_random_uuid(), id, 3, 
       CASE WHEN winner_id = first_inscription_id THEN 6 ELSE 0 END,
       CASE WHEN winner_id = second_inscription_id THEN 6 ELSE 0 END
FROM matches
WHERE winner_id IS NOT NULL AND first_inscription_id IS NOT NULL AND second_inscription_id IS NOT NULL;

-- Update status and result for those migrated matches
UPDATE matches
SET result = '6-0 6-0 6-0',
    status = 'COMPLETED'
WHERE winner_id IS NOT NULL AND first_inscription_id IS NOT NULL AND second_inscription_id IS NOT NULL;
