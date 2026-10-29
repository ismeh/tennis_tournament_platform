package com.tfm.tennis_platform.domain.models.enums;

public enum TournamentStatus {
    DRAFT,          // borrador — creado pero no publicado
    OPEN,           // inscripciones abiertas
    CLOSED,         // inscripciones cerradas
    IN_PROGRESS,    // torneo en juego
    COMPLETED,      // finalizado
    CANCELLED;

    public boolean canTransitionTo(TournamentStatus next) {
        return this != next;
    }
}