ALTER TABLE ref_age_category ADD COLUMN organizer_id UUID NULL;

ALTER TABLE ref_age_category ADD CONSTRAINT fk_ref_age_category_organizer
    FOREIGN KEY (organizer_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_ref_age_category_organizer ON ref_age_category(organizer_id);
