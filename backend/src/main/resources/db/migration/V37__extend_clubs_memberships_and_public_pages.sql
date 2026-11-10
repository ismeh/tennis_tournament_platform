-- Extend clubs into customizable organizational tenants with members, roles and permissions.

ALTER TABLE clubs ADD COLUMN IF NOT EXISTS slug VARCHAR(180);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS tagline VARCHAR(180);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS primary_color VARCHAR(7);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS secondary_color VARCHAR(7);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS city VARCHAR(120);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS country VARCHAR(120);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS contact_email VARCHAR(180);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS phone VARCHAR(40);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS website VARCHAR(300);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS instagram_url VARCHAR(300);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(300);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS x_url VARCHAR(300);
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS founded_year INTEGER;
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS public_page_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE clubs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Backfill slugs for clubs created before public pages existed.
UPDATE clubs
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g')) || '-' || SUBSTRING(REPLACE(id::text, '-', ''), 1, 8)
WHERE slug IS NULL OR TRIM(slug) = '';

ALTER TABLE clubs ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_clubs_slug ON clubs (slug);

CREATE TABLE IF NOT EXISTS club_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    member_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (club_id, member_id)
);

CREATE INDEX IF NOT EXISTS idx_club_memberships_club ON club_memberships (club_id);
CREATE INDEX IF NOT EXISTS idx_club_memberships_member ON club_memberships (member_id);

CREATE TABLE IF NOT EXISTS club_membership_permissions (
    membership_id UUID NOT NULL REFERENCES club_memberships(id) ON DELETE CASCADE,
    permission VARCHAR(40) NOT NULL,
    PRIMARY KEY (membership_id, permission)
);

ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS club_id UUID REFERENCES clubs(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_tournaments_club ON tournaments (club_id);
