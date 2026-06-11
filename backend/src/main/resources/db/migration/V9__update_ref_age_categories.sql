ALTER TABLE ref_age_category
ADD COLUMN IF NOT EXISTS display_order INTEGER;

WITH desired(display_order, category, description, aliases) AS (
    VALUES
        (1, 'Prebenjamín', '8 años', ARRAY['Pre-Benjamin', 'Prebenjamín']),
        (2, 'Benjamín', '9-10 años', ARRAY['Benjamines', 'Benjamín']),
        (3, 'Sub-10', 'Sub-10', ARRAY['Sub-10']),
        (4, 'Alevín Sub-12', '11-12 años', ARRAY['Alevines', 'Alevín Sub-12']),
        (5, 'Infantil Sub-14', '13-14 años', ARRAY['Infantiles', 'Infantil Sub-14']),
        (6, 'Cadete Sub-16', '15-16 años', ARRAY['Cadetes', 'Cadete Sub-16']),
        (7, 'Junior Sub-18', '17-18 años', ARRAY['Juniors', 'Junior Sub-18']),
        (8, 'Absoluta', NULL, ARRAY['ABSOLUTA', 'Absoluta']),
        (9, 'Veterana +30', '30+ años', ARRAY['Veteranos+30', 'Veterana +30']),
        (10, 'Veterana +35', '35+ años', ARRAY['Veteranos+35', 'Veterana +35']),
        (11, 'Veterana +40', '40+ años', ARRAY['Veteranos+40', 'Veterana +40']),
        (12, 'Veterana +45', '45+ años', ARRAY['Veteranos+45', 'Veterana +45']),
        (13, 'Veterana +50', '50+ años', ARRAY['Veteranos+50', 'Veterana +50']),
        (14, 'Veterana +55', '55+ años', ARRAY['Veteranos+55', 'Veterana +55']),
        (15, 'Veterana +60', '60+ años', ARRAY['Veteranos+60', 'Veterana +60']),
        (16, 'Veterana +65', '65+ años', ARRAY['Veteranos+65', 'Veterana +65']),
        (17, 'Veterana +70', '70+ años', ARRAY['Veteranos+70', 'Veterana +70']),
        (18, 'Veterana +75', '75+ años', ARRAY['Veteranos+75', 'Veterana +75']),
        (19, 'Veterana +80', '80+ años', ARRAY['Veteranos+80', 'Veterana +80']),
        (20, 'Veterana +85', '85+ años', ARRAY['Veteranos+85', 'Veterana +85']),
        (21, 'Veterana +90', '90+ años', ARRAY['Veteranos+90', 'Veterana +90'])
)
UPDATE ref_age_category target
SET category = desired.category,
    description = desired.description,
    display_order = desired.display_order
FROM desired
WHERE target.category = ANY(desired.aliases);

WITH desired(display_order, category, description) AS (
    VALUES
        (1, 'Prebenjamín', '8 años'),
        (2, 'Benjamín', '9-10 años'),
        (3, 'Sub-10', 'Sub-10'),
        (4, 'Alevín Sub-12', '11-12 años'),
        (5, 'Infantil Sub-14', '13-14 años'),
        (6, 'Cadete Sub-16', '15-16 años'),
        (7, 'Junior Sub-18', '17-18 años'),
        (8, 'Absoluta', NULL),
        (9, 'Veterana +30', '30+ años'),
        (10, 'Veterana +35', '35+ años'),
        (11, 'Veterana +40', '40+ años'),
        (12, 'Veterana +45', '45+ años'),
        (13, 'Veterana +50', '50+ años'),
        (14, 'Veterana +55', '55+ años'),
        (15, 'Veterana +60', '60+ años'),
        (16, 'Veterana +65', '65+ años'),
        (17, 'Veterana +70', '70+ años'),
        (18, 'Veterana +75', '75+ años'),
        (19, 'Veterana +80', '80+ años'),
        (20, 'Veterana +85', '85+ años'),
        (21, 'Veterana +90', '90+ años')
)
INSERT INTO ref_age_category (category, description, display_order)
SELECT desired.category, desired.description, desired.display_order
FROM desired
WHERE NOT EXISTS (
    SELECT 1
    FROM ref_age_category existing
    WHERE existing.category = desired.category
)
ORDER BY desired.display_order;
