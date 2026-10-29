CREATE TABLE player_invitations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_id      UUID NOT NULL UNIQUE REFERENCES participants(id) ON DELETE CASCADE,
    token_hash          VARCHAR(128) NOT NULL UNIQUE,
    expires_at          TIMESTAMPTZ NOT NULL,
    claimed_at          TIMESTAMPTZ,
    claimed_by_member   UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_player_invitations_token ON player_invitations(token_hash);
CREATE INDEX idx_player_invitations_participant ON player_invitations(participant_id);
