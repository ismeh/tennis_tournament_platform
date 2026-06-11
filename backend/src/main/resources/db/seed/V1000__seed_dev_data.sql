INSERT INTO persons (tennis_id, first_name, last_name, nationality, birth_date, gender) VALUES
    ('IPIN001', 'Rafael', 'Nadal', 'ESP', DATE '1986-06-03', 'MALE'),
    ('IPIN002', 'Roger', 'Federer', 'SUI', DATE '1981-08-08', 'MALE'),
    ('IPIN003', 'Serena', 'Williams', 'USA', DATE '1981-09-26', 'FEMALE'),
    ('IPIN004', 'Iga', 'Swiatek', 'POL', DATE '2001-05-31', 'FEMALE'),
    ('IPIN005', 'Novak', 'Djokovic', 'SRB', DATE '1987-05-22', 'MALE'),
    ('IPIN006', 'Coco', 'Gauff', 'USA', DATE '2004-03-13', 'FEMALE')
ON CONFLICT (tennis_id) DO NOTHING;

INSERT INTO users (email, password_hash, token_hash, email_verified, tier, person_id) VALUES
    (
        'rafa@example.com',
        '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
        NULL,
        TRUE,
        'ADVANCED',
        (SELECT id FROM persons WHERE tennis_id = 'IPIN001' LIMIT 1)
    ),
    (
        'roger@example.com',
        '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
        NULL,
        TRUE,
        'ADVANCED',
        (SELECT id FROM persons WHERE tennis_id = 'IPIN002' LIMIT 1)
    ),
    (
        'serena@example.com',
        '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
        NULL,
        TRUE,
        'ADVANCED',
        (SELECT id FROM persons WHERE tennis_id = 'IPIN003' LIMIT 1)
    ),
    (
        'iga@example.com',
        '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
        NULL,
        TRUE,
        'ADVANCED',
        (SELECT id FROM persons WHERE tennis_id = 'IPIN004' LIMIT 1)
    ),
    (
        'novak@example.com',
        '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
        NULL,
        TRUE,
        'ADVANCED',
        (SELECT id FROM persons WHERE tennis_id = 'IPIN005' LIMIT 1)
    ),
    (
        'coco@example.com',
        '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
        NULL,
        TRUE,
        'INTERMEDIATE',
        (SELECT id FROM persons WHERE tennis_id = 'IPIN006' LIMIT 1)
    )
ON CONFLICT (email) DO NOTHING;

INSERT INTO tournaments (
    name,
    start_date,
    end_date,
    inscription_start_date,
    inscription_end_date,
    venue,
    location,
    country,
    surface,
    category,
    max_players,
    state,
    created_by
)
SELECT
    'Open de Primavera 2026',
    DATE '2026-05-01',
    DATE '2026-05-15',
    DATE '2026-03-01',
    DATE '2026-04-25',
    'Club de Tenis Principal',
    'Club de Tenis Principal',
    'ESP',
    'CLAY',
    'NATIONAL',
    128,
    'OPEN',
    (SELECT id FROM users WHERE email = 'rafa@example.com' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM tournaments WHERE name = 'Open de Primavera 2026'
);

INSERT INTO events (tournament_id, age_category_id, name, discipline, event_type, gender, draw_size)
SELECT
    (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026' LIMIT 1),
    (SELECT id FROM ref_age_category WHERE category = 'Absoluta' LIMIT 1),
    source.name,
    'TENNIS',
    source.event_type,
    source.gender,
    source.draw_size
FROM (VALUES
    ('Absoluto Individual Masculino', 'SINGLES', 'MALE', 32),
    ('Absoluto Dobles Mixto', 'DOUBLES', 'MIXED', 16)
) AS source(name, event_type, gender, draw_size)
WHERE NOT EXISTS (
    SELECT 1 FROM events WHERE events.name = source.name
);

INSERT INTO participants (tournament_id, person_id, participant_type, entry_status, seed)
SELECT
    (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026' LIMIT 1),
    person.id,
    'INDIVIDUAL',
    'DIRECT_ACCEPTANCE',
    source.seed
FROM (VALUES
    ('IPIN001', 1),
    ('IPIN002', 2),
    ('IPIN005', 3)
) AS source(tennis_id, seed)
JOIN persons person ON person.tennis_id = source.tennis_id
WHERE NOT EXISTS (
    SELECT 1
    FROM participants participant
    WHERE participant.tournament_id = (SELECT id FROM tournaments WHERE name = 'Open de Primavera 2026' LIMIT 1)
      AND participant.person_id = person.id
);

INSERT INTO stages (event_id, stage_order, stage_type)
SELECT
    (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino' LIMIT 1),
    1,
    'MAIN'
WHERE NOT EXISTS (
    SELECT 1
    FROM stages
    WHERE event_id = (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino' LIMIT 1)
      AND stage_order = 1
);

INSERT INTO draws (stage_id, draw_type, label)
SELECT
    (SELECT id FROM stages WHERE event_id = (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino' LIMIT 1) LIMIT 1),
    'ELIMINATION',
    'Main Draw'
WHERE NOT EXISTS (
    SELECT 1
    FROM draws
    WHERE stage_id = (SELECT id FROM stages WHERE event_id = (SELECT id FROM events WHERE name = 'Absoluto Individual Masculino' LIMIT 1) LIMIT 1)
      AND label = 'Main Draw'
);

INSERT INTO matches (draw_id, round_number, scheduled_at, court, result)
SELECT
    (SELECT id FROM draws WHERE label = 'Main Draw' LIMIT 1),
    1,
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM matches
    WHERE draw_id = (SELECT id FROM draws WHERE label = 'Main Draw' LIMIT 1)
      AND round_number = 1
);
