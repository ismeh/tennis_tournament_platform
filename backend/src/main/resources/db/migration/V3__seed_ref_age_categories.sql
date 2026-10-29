INSERT INTO ref_age_category (category, description)
SELECT category, description
FROM (VALUES
    ('Pre-Benjamin', '8 años'),
    ('Benjamines', '9-10 años'),
    ('Alevines', '11-12 años'),
    ('Infantiles', '13-14 años'),
    ('Cadetes', '15-16 años'),
    ('Juniors', '17-18 años'),
    ('ABSOLUTA', NULL),
    ('Veteranos+30', '30-34 años'),
    ('Veteranos+35', '35-39 años'),
    ('Veteranos+40', '40-44 años'),
    ('Veteranos+45', '45-49 años'),
    ('Veteranos+50', '50-54 años'),
    ('Veteranos+55', '55-59 años'),
    ('Veteranos+60', '60-64 años')
) AS source(category, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM ref_age_category existing
    WHERE existing.category = source.category
);
