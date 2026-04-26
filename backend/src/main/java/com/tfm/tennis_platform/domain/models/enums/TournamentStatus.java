package com.tfm.tennis_platform.domain.models.enums;

public enum TournamentStatus {
    DRAFT,          // borrador — creado pero no publicado
    OPEN,           // inscripciones abiertas
    CLOSED,         // inscripciones cerradas
    IN_PROGRESS,    // torneo en juego
    COMPLETED,      // finalizado
    CANCELLED;

    public boolean canTransitionTo(TournamentStatus next) {
        return switch (this) {
            case DRAFT       -> next == OPEN || next == CANCELLED;
            case OPEN        -> next == CLOSED || next == CANCELLED;
            case CLOSED      -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED || next == CANCELLED;
            default          -> false;
        };
    }
}