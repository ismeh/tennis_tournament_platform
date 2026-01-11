-- Tablas Principales
CREATE TABLE members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255),
    gender CHAR(1) NOT NULL CHECK (gender IN ('H', 'M')),
    tier VARCHAR(20) DEFAULT 'free' CHECK (tier IN ('free', 'intermediate', 'advanced')),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    genre CHAR(1) NOT NULL CHECK (genre IN ('H', 'M', 'X')),
    mode VARCHAR(10) NOT NULL CHECK (mode IN ('single', 'doubles'))
);

CREATE TABLE tournaments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description CLOB, -- CLOB es el equivalente a TEXT en H2
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    max_players INTEGER NOT NULL,
    location VARCHAR(255),
    state VARCHAR(20) DEFAULT 'soon' CHECK (state IN ('soon', 'inscription', 'playing', 'finished')),
    created_by INTEGER,
    CONSTRAINT fk_tournament_creator FOREIGN KEY (created_by) REFERENCES members(id)
);

-- Tablas Relacionales e Inscripciones
CREATE TABLE tournament_categories (
     id INT AUTO_INCREMENT PRIMARY KEY,
     tournament_id INTEGER NOT NULL,
     category_id INTEGER NOT NULL,
     CONSTRAINT fk_tc_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
     CONSTRAINT fk_tc_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE inscriptions (
     id INT AUTO_INCREMENT PRIMARY KEY,
     tournament_id INTEGER NOT NULL,
     category_id INTEGER NOT NULL,
     member_id INTEGER NOT NULL,
     partner_id INTEGER, -- NULL si es single
     role_id INTEGER,
     registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT fk_ins_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
     CONSTRAINT fk_ins_category FOREIGN KEY (category_id) REFERENCES categories(id),
     CONSTRAINT fk_ins_member FOREIGN KEY (member_id) REFERENCES members(id),
     CONSTRAINT fk_ins_partner FOREIGN KEY (partner_id) REFERENCES members(id),
     CONSTRAINT fk_ins_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Partidos e Historial
CREATE TABLE matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tournament_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    first_inscription_id INTEGER,
    second_inscription_id INTEGER,
    winner_id INTEGER,
    round_number INTEGER NOT NULL,
    next_match_id INTEGER,
    scheduled_at TIMESTAMP,
    court VARCHAR(50),
    result VARCHAR(100),
    CONSTRAINT fk_match_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_match_ins1 FOREIGN KEY (first_inscription_id) REFERENCES inscriptions(id),
    CONSTRAINT fk_match_ins2 FOREIGN KEY (second_inscription_id) REFERENCES inscriptions(id),
    CONSTRAINT fk_match_winner FOREIGN KEY (winner_id) REFERENCES inscriptions(id),
    CONSTRAINT fk_match_next FOREIGN KEY (next_match_id) REFERENCES matches(id)
);