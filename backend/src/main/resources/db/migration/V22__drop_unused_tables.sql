-- Drop unused tables identified during code review

-- Matchup ecosystem (replaced by matches, never wired into app logic)
DROP TABLE IF EXISTS sets;
DROP TABLE IF EXISTS matchup_sides;
DROP TABLE IF EXISTS matchups;

-- Categories ecosystem (unused; ref_age_category is the active system)
DROP TABLE IF EXISTS tournament_categories;
DROP TABLE IF EXISTS categories;

-- Rankings (never populated; pro_players and matches-derived ranking are used instead)
DROP TABLE IF EXISTS rankings;
