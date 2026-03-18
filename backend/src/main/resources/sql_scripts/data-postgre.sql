-- 1. Insert sample persons
INSERT INTO persons (tennis_id, first_name, last_name, nationality, birth_date, gender) VALUES
                                                                                            ('IPIN001','Rafael','Nadal','ESP', DATE '1986-06-03','MALE'),
                                                                                            ('IPIN002','Roger','Federer','SUI', DATE '1981-08-08','MALE'),
                                                                                            ('IPIN003','Serena','Williams','USA', DATE '1981-09-26','FEMALE'),
                                                                                            ('IPIN004','Iga','Swiatek','POL', DATE '2001-05-31','FEMALE'),
                                                                                            ('IPIN005','Novak','Djokovic','SRB', DATE '1987-05-22','MALE'),
                                                                                            ('IPIN006','Coco','Gauff','USA', DATE '2004-03-13','FEMALE');

-- 2. Insert sample users
INSERT INTO users (email, password_hash, token_hash, tier, person_id) VALUES
                                                              ('rafa@example.com','$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',NULL,'ADVANCED',
                                                               (SELECT id FROM persons WHERE tennis_id='IPIN001' LIMIT 1)),

                                                              ('roger@example.com','$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',NULL,'ADVANCED',
                                                               (SELECT id FROM persons WHERE tennis_id='IPIN002' LIMIT 1)),

                                                              ('serena@example.com','$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',NULL,'ADVANCED',
                                                               (SELECT id FROM persons WHERE tennis_id='IPIN003' LIMIT 1)),

                                                              ('iga@example.com','$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',NULL,'ADVANCED',
                                                               (SELECT id FROM persons WHERE tennis_id='IPIN004' LIMIT 1)),

                                                              ('novak@example.com','$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',NULL,'ADVANCED',
                                                               (SELECT id FROM persons WHERE tennis_id='IPIN005' LIMIT 1)),

                                                              ('coco@example.com','$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',NULL,'INTERMEDIATE',
                                                               (SELECT id FROM persons WHERE tennis_id='IPIN006' LIMIT 1));

-- 3. Insert tournament
INSERT INTO tournaments (name, start_date, end_date, venue, country, surface, category, status)
VALUES (
           'Open de Primavera 2026',
           DATE '2026-05-01',
           DATE '2026-05-15',
           'Club de Tenis Principal',
           'ESP',
           'CLAY',
           'NATIONAL',
           'ACTIVE'
       );

-- 4. Insert events
INSERT INTO events (tournament_id, name, discipline, event_type, gender, age_category, draw_size) VALUES
                                                                                                      (
                                                                                                          (SELECT id FROM tournaments WHERE name='Open de Primavera 2026' LIMIT 1),
                                                                                                          'Absoluto Individual Masculino',
                                                                                                          'TENNIS',
                                                                                                          'SINGLES',
                                                                                                          'MALE',
                                                                                                          'OPEN',
                                                                                                          32
                                                                                                      ),
                                                                                                      (
                                                                                                          (SELECT id FROM tournaments WHERE name='Open de Primavera 2026' LIMIT 1),
                                                                                                          'Absoluto Dobles Mixto',
                                                                                                          'TENNIS',
                                                                                                          'DOUBLES',
                                                                                                          'MIXED',
                                                                                                          'OPEN',
                                                                                                          16
                                                                                                      );

-- 5. Insert participants
INSERT INTO participants (tournament_id, person_id, participant_type, entry_status, seed) VALUES
                                                                                              (
                                                                                                  (SELECT id FROM tournaments WHERE name='Open de Primavera 2026' LIMIT 1),
                                                                                                  (SELECT id FROM persons WHERE tennis_id='IPIN001' LIMIT 1),
                                                                                                  'INDIVIDUAL',
                                                                                                  'DIRECT_ACCEPTANCE',
                                                                                                  1
                                                                                              ),
                                                                                              (
                                                                                                  (SELECT id FROM tournaments WHERE name='Open de Primavera 2026' LIMIT 1),
                                                                                                  (SELECT id FROM persons WHERE tennis_id='IPIN002' LIMIT 1),
                                                                                                  'INDIVIDUAL',
                                                                                                  'DIRECT_ACCEPTANCE',
                                                                                                  2
                                                                                              ),
                                                                                              (
                                                                                                  (SELECT id FROM tournaments WHERE name='Open de Primavera 2026' LIMIT 1),
                                                                                                  (SELECT id FROM persons WHERE tennis_id='IPIN005' LIMIT 1),
                                                                                                  'INDIVIDUAL',
                                                                                                  'DIRECT_ACCEPTANCE',
                                                                                                  3
                                                                                              );

-- 6. Insert stage
INSERT INTO stages (event_id, stage_number, stage_type)
VALUES (
           (SELECT id FROM events WHERE name='Absoluto Individual Masculino' LIMIT 1),
           1,
           'MAIN'
       );

-- 7. Insert draw
INSERT INTO draws (stage_id, draw_type, draw_name)
VALUES (
           (SELECT id FROM stages
            WHERE event_id = (
                SELECT id FROM events WHERE name='Absoluto Individual Masculino' LIMIT 1
            ) LIMIT 1),
           'ELIMINATION',
           'Main Draw'
       );

-- 8. Insert matchup
INSERT INTO matchups (draw_id, round_number, match_number, match_format, status, scheduled_at, court, winner_side)
VALUES (
           (SELECT id FROM draws WHERE draw_name='Main Draw' LIMIT 1),
           1,
           1,
           'SET3-S:6/TB7',
           'UPCOMING',
           TIMESTAMPTZ '2026-05-10 10:00:00+02',
           'Pista Central',
           NULL
       );

-- 9. Insert matchup sides
INSERT INTO matchup_sides (matchup_id, side_number, participant_id) VALUES
                                                                        (
                                                                            (SELECT id FROM matchups WHERE round_number=1 AND match_number=1 LIMIT 1),
                                                                            1,
                                                                            (SELECT id FROM participants
                                                                             WHERE person_id=(SELECT id FROM persons WHERE tennis_id='IPIN001' LIMIT 1)
                                                                             LIMIT 1)
                                                                        ),
                                                                        (
                                                                            (SELECT id FROM matchups WHERE round_number=1 AND match_number=1 LIMIT 1),
                                                                            2,
                                                                            (SELECT id FROM participants
                                                                             WHERE person_id=(SELECT id FROM persons WHERE tennis_id='IPIN005' LIMIT 1)
                                                                             LIMIT 1)
                                                                        );