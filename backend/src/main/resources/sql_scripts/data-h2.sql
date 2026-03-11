SET MODE PostgreSQL;

-- 1. Insert sample persons
    INSERT INTO persons (tennis_id, first_name, last_name, nationality, birth_date, gender) VALUES
        ( 'IPIN001', 'Rafael', 'Nadal', 'ESP', '1986-06-03', 'MALE'),
        ( 'IPIN002', 'Roger', 'Federer', 'SUI', '1981-08-08', 'MALE'),
        ( 'IPIN003', 'Serena', 'Williams', 'USA', '1981-09-26', 'FEMALE'),
        ( 'IPIN004', 'Iga', 'Swiatek', 'POL', '2001-05-31', 'FEMALE'),
        ( 'IPIN005', 'Novak', 'Djokovic', 'SRB', '1987-05-22', 'MALE'),
        ( 'IPIN006', 'Coco', 'Gauff', 'USA', '2004-03-13', 'FEMALE');

    -- 2. Insert sample users (linked to persons)
    INSERT INTO users (email, password_hash, tier, person_id) VALUES
        ( 'rafa@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN001')),
        ( 'roger@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN002')),
        ( 'serena@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN003')),
        ( 'iga@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN004')),
        ( 'novak@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN005')),
        ( 'coco@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'INTERMEDIATE', (SELECT id FROM persons WHERE tennis_id = 'IPIN006'));

    -- 3. Insert a tournament
    INSERT INTO tournaments (name, start_date, end_date, venue, country, surface, category, status) VALUES
        ( 'Open de Primavera 2026', '2026-05-01', '2026-05-15', 'Club de Tenis Principal', 'ESP', 'CLAY', 'NATIONAL', 'ACTIVE');

    -- 4. Insert events (categories)
    INSERT INTO events (tournament_id, name, discipline, event_type, gender, age_category, draw_size) VALUES
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), 'Absoluto Individual Masculino', 'TENNIS', 'SINGLES', 'MALE', 'OPEN', 32),
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), 'Absoluto Dobles Mixto', 'TENNIS', 'DOUBLES', 'MIXED', 'OPEN', 16);

    -- 5. Insert participants (individuals)
    INSERT INTO participants (tournament_id, person_id, participant_type, entry_status, seed) VALUES
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), (SELECT id FROM persons WHERE tennis_id = 'IPIN001'), 'INDIVIDUAL', 'DIRECT_ACCEPTANCE', 1),
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), (SELECT id FROM persons WHERE tennis_id = 'IPIN002'), 'INDIVIDUAL', 'DIRECT_ACCEPTANCE', 2),
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), (SELECT id FROM persons WHERE tennis_id = 'IPIN005'), 'INDIVIDUAL', 'DIRECT_ACCEPTANCE', 3);

    -- 6. Insert a stage for singles event
    INSERT INTO stages (event_id, stage_number, stage_type) VALUES
        ( (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino'), 1, 'MAIN');

    -- 7. Insert a draw for the stage
    INSERT INTO draws (stage_id, draw_type, draw_name) VALUES
        ( (SELECT id FROM stages WHERE event_id = (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino')), 'ELIMINATION', 'Main Draw');

    -- 8. Insert a matchup (Rafa vs Novak)
    INSERT INTO matchups (draw_id, round_number, match_number, match_format, status, scheduled_at, court, winner_side) VALUES
        ( (SELECT id FROM draws WHERE draw_name = 'Main Draw'), 1, 1, 'SET3-S:6/TB7', 'UPCOMING', '2024-05-10 10:00:00+02', 'Pista Central', NULL);

    -- 9. Insert matchup sides
    INSERT INTO matchup_sides (matchup_id, side_number, participant_id) VALUES
        ( (SELECT id FROM matchups WHERE round_number = 1 AND match_number = 1), 1, (SELECT id FROM participants WHERE person_id = (SELECT id FROM persons WHERE tennis_id = 'IPIN001'))),
        ( (SELECT id FROM matchups WHERE round_number = 1 AND match_number = 1), 2, (SELECT id FROM participants WHERE person_id = (SELECT id FROM persons WHERE tennis_id = 'IPIN005')));