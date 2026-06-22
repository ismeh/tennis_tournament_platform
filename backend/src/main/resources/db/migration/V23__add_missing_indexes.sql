-- Add missing indexes identified during code review

-- participants.person_id (FK without index)
CREATE INDEX idx_participants_person ON participants(person_id);

-- events.age_category_id (FK without index)
CREATE INDEX idx_events_age_category ON events(age_category_id);

-- matches.court_id (FK without index)
CREATE INDEX idx_matches_court ON matches(court_id);

-- matches.round_number (queries by round)
CREATE INDEX idx_matches_round ON matches(round_number);

-- tournaments.state (filter by status)
CREATE INDEX idx_tournaments_state ON tournaments(state);

-- tournaments.start_date (date range queries)
CREATE INDEX idx_tournaments_start_date ON tournaments(start_date);

-- users.person_id (FK without index)
CREATE INDEX idx_users_person ON users(person_id);

-- users.user_role (filter by role)
CREATE INDEX idx_users_user_role ON users(user_role);
