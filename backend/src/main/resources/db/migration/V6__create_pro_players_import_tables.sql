CREATE TABLE IF NOT EXISTS pro_players (
    id SERIAL PRIMARY KEY,
    puntos INTEGER NOT NULL,
    licencia VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    posicion INTEGER NOT NULL,
    posicion_territorial INTEGER NOT NULL,
    posicion_provincial INTEGER NOT NULL,
    posicion_club INTEGER NOT NULL,
    posicion_edad INTEGER NOT NULL,
    edad VARCHAR(50) NOT NULL,
    nombre_club VARCHAR(255) NOT NULL,
    nombre_provincial VARCHAR(255) NOT NULL,
    nombre_territorial VARCHAR(255) NOT NULL,
    nombre_categoria VARCHAR(255) NOT NULL,
    puntos_otorga INTEGER NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    gender VARCHAR(10) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pro_players_gender_position
ON pro_players (gender, posicion);

CREATE INDEX IF NOT EXISTS idx_pro_players_license
ON pro_players (licencia)
WHERE licencia IS NOT NULL;

CREATE TABLE IF NOT EXISTS pro_player_imports (
    id SERIAL PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    row_count INTEGER NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_pro_player_imports_source_checksum_status
ON pro_player_imports (source_name, checksum, status);
