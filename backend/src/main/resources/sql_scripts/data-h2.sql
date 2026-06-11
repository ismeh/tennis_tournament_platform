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
	    INSERT INTO users (email, password_hash, token_hash, email_verified, tier, person_id) VALUES
	        ( 'rafa@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', NULL, TRUE, 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN001')),
	        ( 'roger@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', NULL, TRUE, 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN002')),
	        ( 'serena@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', NULL, TRUE, 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN003')),
	        ( 'iga@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', NULL, TRUE, 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN004')),
	        ( 'novak@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', NULL, TRUE, 'ADVANCED', (SELECT id FROM persons WHERE tennis_id = 'IPIN005')),
	        ( 'coco@example.com', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', NULL, TRUE, 'INTERMEDIATE', (SELECT id FROM persons WHERE tennis_id = 'IPIN006'));

    -- 3. Insert a tournament
    INSERT INTO tournaments (name, start_date, end_date, venue, country, surface, category, state) VALUES
        ( 'Open de Primavera 2026', '2026-05-01', '2026-05-15', 'Club de Tenis Principal', 'ESP', 'CLAY', 'NATIONAL', 'OPEN');

    -- 3.5. Insert age category references
    INSERT INTO ref_age_category (category, description, display_order) VALUES
        ('Prebenjamín', '8 anos', 1),
        ('Benjamín', '9-10 anos', 2),
        ('Sub-10', 'Sub-10', 3),
        ('Alevín Sub-12', '11-12 anos', 4),
        ('Infantil Sub-14', '13-14 anos', 5),
        ('Cadete Sub-16', '15-16 anos', 6),
        ('Junior Sub-18', '17-18 anos', 7),
        ('Absoluta', NULL, 8),
        ('Veterana +30', '30+ anos', 9),
        ('Veterana +35', '35+ anos', 10),
        ('Veterana +40', '40+ anos', 11),
        ('Veterana +45', '45+ anos', 12),
        ('Veterana +50', '50+ anos', 13),
        ('Veterana +55', '55+ anos', 14),
        ('Veterana +60', '60+ anos', 15),
        ('Veterana +65', '65+ anos', 16),
        ('Veterana +70', '70+ anos', 17),
        ('Veterana +75', '75+ anos', 18),
        ('Veterana +80', '80+ anos', 19),
        ('Veterana +85', '85+ anos', 20),
        ('Veterana +90', '90+ anos', 21);

    -- 4. Insert events (categories)
    INSERT INTO events (tournament_id, age_category_id, name, discipline, event_type, gender, draw_size) VALUES
        (
            (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'),
            (SELECT id FROM ref_age_category WHERE category = 'Absoluta'),
            'Absoluto Individual Masculino',
            'TENNIS',
            'SINGLES',
            'MALE',
            32
        ),
        (
            (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'),
            (SELECT id FROM ref_age_category WHERE category = 'Absoluta'),
            'Absoluto Dobles Mixto',
            'TENNIS',
            'DOUBLES',
            'MIXED',
            16
        );

    -- 5. Insert participants (individuals)
    INSERT INTO participants (tournament_id, person_id, participant_type, entry_status, seed) VALUES
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), (SELECT id FROM persons WHERE tennis_id = 'IPIN001'), 'INDIVIDUAL', 'DIRECT_ACCEPTANCE', 1),
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), (SELECT id FROM persons WHERE tennis_id = 'IPIN002'), 'INDIVIDUAL', 'DIRECT_ACCEPTANCE', 2),
        ( (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026'), (SELECT id FROM persons WHERE tennis_id = 'IPIN005'), 'INDIVIDUAL', 'DIRECT_ACCEPTANCE', 3);

    -- 6. Insert a stage for singles event
    INSERT INTO stages (event_id, stage_order, stage_type) VALUES
        ( (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino'), 1, 'MAIN');

    -- 7. Insert a draw for the stage
    INSERT INTO draws (stage_id, draw_type, label) VALUES
        ( (SELECT id FROM stages WHERE event_id = (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino')), 'ELIMINATION', 'Main Draw');

    -- 8. Insert an empty match structure for the MVP
    INSERT INTO matches (draw_id, round_number, bracket_position, scheduled_at, court, result) VALUES
        ( (SELECT id FROM draws WHERE label = 'Main Draw'), 1, 0, NULL, NULL, NULL);
