CREATE TABLE tournament_umpires (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL,
    umpire_id UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournament_umpires_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    CONSTRAINT fk_tournament_umpires_umpire FOREIGN KEY (umpire_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_tournament_umpire UNIQUE (tournament_id, umpire_id)
);

CREATE INDEX idx_tournament_umpires_tournament_id ON tournament_umpires(tournament_id);
CREATE INDEX idx_tournament_umpires_umpire_id ON tournament_umpires(umpire_id);
