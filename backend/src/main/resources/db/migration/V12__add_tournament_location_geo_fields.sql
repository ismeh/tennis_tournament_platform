ALTER TABLE tournaments
    ADD COLUMN IF NOT EXISTS location_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS location_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS location_place_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS location_formatted_address VARCHAR(500);
