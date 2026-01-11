-- Creación de tipos ENUM para mayor control
CREATE TYPE user_gender AS ENUM ('H', 'M');
CREATE TYPE category_genre AS ENUM ('H', 'M', 'X');
CREATE TYPE category_mode AS ENUM ('single', 'doubles');
CREATE TYPE member_tier AS ENUM ('free', 'intermediate', 'advanced');
CREATE TYPE tournament_state AS ENUM ('soon', 'inscription', 'playing', 'finished');

-- Tablas Principales
CREATE TABLE members (
                         id SERIAL PRIMARY KEY,
                         email VARCHAR(255) UNIQUE NOT NULL,
                         username VARCHAR(100) NOT NULL,
                         password VARCHAR(255),
                         gender user_gender NOT NULL,
                         tier member_tier DEFAULT 'free',
                         registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL
);

CREATE TABLE categories (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            genre category_genre NOT NULL,
                            mode category_mode NOT NULL
);

CREATE TABLE tournaments (
                             id SERIAL PRIMARY KEY,
                             name VARCHAR(150) NOT NULL,
                             description TEXT,
                             start_date DATE NOT NULL,
                             end_date DATE NOT NULL,
                             max_players INTEGER NOT NULL,
                             location VARCHAR(255),
                             state tournament_state DEFAULT 'soon',
                             created_by INTEGER REFERENCES members(id)
);

-- Tablas Relacionales e Inscripciones
CREATE TABLE tournament_categories (
                                       id SERIAL PRIMARY KEY,
                                       tournament_id INTEGER REFERENCES tournaments(id) ON DELETE CASCADE,
                                       category_id INTEGER REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE inscriptions (
                              id SERIAL PRIMARY KEY,
                              tournament_id INTEGER REFERENCES tournaments(id) ON DELETE CASCADE,
                              category_id INTEGER REFERENCES categories(id), -- Añadido para saber en qué categoría compite
                              member_id INTEGER REFERENCES members(id),
                              partner_id INTEGER REFERENCES members(id), -- NULL si es single
                              role_id INTEGER REFERENCES roles(id),
                              registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Partidos e Historial
CREATE TABLE matches (
                         id SERIAL PRIMARY KEY,
                         tournament_id INTEGER REFERENCES tournaments(id) ON DELETE CASCADE,
                         category_id INTEGER REFERENCES categories(id),
                         first_inscription_id INTEGER REFERENCES inscriptions(id),
                         second_inscription_id INTEGER REFERENCES inscriptions(id),
                         winner_id INTEGER REFERENCES inscriptions(id),
                         round_number INTEGER NOT NULL, -- 1=Final, 2=Semis, 3=Cuartos...
                         next_match_id INTEGER REFERENCES matches(id), -- Para lógica de brackets
                         scheduled_at TIMESTAMP,
                         court VARCHAR(50),
                         result VARCHAR(100) -- Ej: "6-4 / 7-5"
);

------ Insertar Datos de Ejemplo
-- 1. Insertar Roles
INSERT INTO roles (name) VALUES ('Organizer'), ('Player'), ('Referee');

-- 2. Insertar Categorías
INSERT INTO categories (name, genre, mode) VALUES
   ('Absoluto Individual Masculino', 'H', 'single'),
   ('Absoluto Individual Femenino', 'M', 'single'),
   ('Absoluto Dobles Masculino', 'H', 'doubles'),
   ('Absoluto Dobles Femenino', 'M', 'doubles'),
   ('Absoluto Dobles Mixto', 'X', 'doubles');

-- 3. Insertar Miembros (6 jugadores)
INSERT INTO members (email, username, password, gender, tier) VALUES
    ('rafa@example.com', 'RafaNadal', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',  'H', 'advanced'),
    ('roger@example.com', 'RogerF', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',  'H', 'advanced'),
    ('serena@example.com', 'SerenaW', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',  'M', 'advanced'),
    ('iga@example.com', 'IgaS', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',  'M', 'advanced'),
    ('novak@example.com', 'Djoko', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',  'H', 'advanced'),
    ('coco@example.com', 'CocoG', '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',  'M', 'intermediate');
-- password

-- 4. Insertar un Torneo
INSERT INTO tournaments (name, description, start_date, end_date, max_players, location, state, created_by) VALUES
    ('Open de Primavera 202', 'Torneo inaugural de la temporada', '2026-05-01', '2026-05-15', 32, 'Club de Tenis Principal', 'inscription', 1);

-- 5. Insertar Categorías para ese Torneo
INSERT INTO tournament_categories (tournament_id, category_id) VALUES
                                                                   (1, 1), -- Single Masculino
                                                                   (1, 5); -- Dobles Mixto

-- 6. Insertar Inscripciones
-- Inscripción Individual (Rafa en Single Masculino)
INSERT INTO inscriptions (tournament_id, category_id, member_id, partner_id, role_id)
VALUES (1, 1, 1, NULL, 2);

-- Inscripción en Pareja (Roger e Iga en Dobles Mixto)
INSERT INTO inscriptions (tournament_id, category_id, member_id, partner_id, role_id)
VALUES (1, 5, 2, 4, 2);

-- 7. Insertar un Partido de ejemplo (Rafa vs Novak)
-- Primero inscribimos a Novak para que pueda jugar
INSERT INTO inscriptions (tournament_id, category_id, member_id, partner_id, role_id)
VALUES (1, 1, 5, NULL, 2);

-- Ahora el partido (Asumiendo que Rafa es Inscripción 1 y Novak es Inscripción 3)
INSERT INTO matches (tournament_id, category_id, first_inscription_id, second_inscription_id, round_number, scheduled_at, court, result)
VALUES (1, 1, 1, 3, 1, '2024-05-10 10:00:00', 'Pista Central', '6-4 / 6-4');
