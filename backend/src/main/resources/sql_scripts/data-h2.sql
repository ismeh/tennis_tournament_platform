-- 1. Insertar Roles
INSERT INTO roles (name) VALUES
    ('Organizer'),
    ('Player'),
    ('Referee');

-- 2. Insertar Categorías
INSERT INTO categories (name, genre, mode) VALUES
   ('Absoluto Individual Masculino', 'H', 'single'),
   ('Absoluto Individual Femenino', 'M', 'single'),
   ('Absoluto Dobles Masculino', 'H', 'doubles'),
   ('Absoluto Dobles Femenino', 'M', 'doubles'),
   ('Absoluto Dobles Mixto', 'X', 'doubles');

-- 3. Insertar Miembros
INSERT INTO members (email, username, password, gender, tier) VALUES
    ('rafa@example.com', 'RafaNadal', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'H', 'advanced'),
    ('roger@example.com', 'RogerF', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'H', 'advanced'),
    ('serena@example.com', 'SerenaW', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'M', 'advanced'),
    ('iga@example.com', 'IgaS', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'M', 'advanced'),
    ('novak@example.com', 'Djoko', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'H', 'advanced'),
    ('coco@example.com', 'CocoG', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna', 'M', 'intermediate');
--password

-- 4. Insertar un Torneo
INSERT INTO tournaments (name, description, start_date, end_date, max_players, location, state, created_by) VALUES
    ('Open de Primavera 202', 'Torneo inaugural de la temporada', DATE '2026-05-01', DATE '2026-05-15', 32, 'Club de Tenis Principal', 'inscription', 1);

-- 5. Categorías del Torneo
INSERT INTO tournament_categories (tournament_id, category_id) VALUES
                                                                   (1, 1),
                                                                   (1, 5);

-- 6. Inscripciones
INSERT INTO inscriptions (tournament_id, category_id, member_id, partner_id, role_id)
VALUES (1, 1, 1, NULL, 2);

INSERT INTO inscriptions (tournament_id, category_id, member_id, partner_id, role_id)
VALUES (1, 5, 2, 4, 2);

-- 7. Inscribir a Novak
INSERT INTO inscriptions (tournament_id, category_id, member_id, partner_id, role_id)
VALUES (1, 1, 5, NULL, 2);

-- 8. Partido (Rafa vs Novak)
INSERT INTO matches (tournament_id, category_id, first_inscription_id, second_inscription_id, round_number, scheduled_at, court, result)
VALUES (1, 1, 1, 3, 1, TIMESTAMP '2024-05-10 10:00:00', 'Pista Central', '6-4 / 6-4');
