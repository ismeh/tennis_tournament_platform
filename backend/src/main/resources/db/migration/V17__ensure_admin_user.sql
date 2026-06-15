INSERT INTO users (email, password_hash, token_hash, email_verified, tier, user_role, person_id)
VALUES (
    'admin@example.com',
    '$2a$12$ddhS2ajAtCpi3QDQm4LB2.hHy5kyRphe8SoYh56Unfwv4ToqStDna',
    NULL,
    TRUE,
    'ADVANCED',
    'ADMIN',
    NULL
)
ON CONFLICT (email) DO UPDATE
SET user_role = 'ADMIN',
    tier = 'ADVANCED',
    person_id = NULL;
