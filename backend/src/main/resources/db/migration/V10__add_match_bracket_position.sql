ALTER TABLE matches
    ADD COLUMN IF NOT EXISTS bracket_position INTEGER;

UPDATE matches
SET bracket_position = ranked.position
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY draw_id, round_number ORDER BY id) - 1 AS position
    FROM matches
) ranked
WHERE matches.id = ranked.id
  AND matches.bracket_position IS NULL;
