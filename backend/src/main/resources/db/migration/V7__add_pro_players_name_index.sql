CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_pro_players_name_trgm
ON pro_players USING GIN (LOWER(name) gin_trgm_ops);
