package com.tfm.tennis_platform.domain.models.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TournamentStatus")
class TournamentStatusTest {

    @Test
    @DisplayName("DRAFT can transition to OPEN and CANCELLED only")
    void draftTransitions() {
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.OPEN));
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.CLOSED));
        assertFalse(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertFalse(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.COMPLETED));
        assertFalse(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.DRAFT));
    }

    @Test
    @DisplayName("OPEN can transition to CLOSED and CANCELLED only")
    void openTransitions() {
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.CLOSED));
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.OPEN));
        assertFalse(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertFalse(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.COMPLETED));
        assertFalse(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.DRAFT));
    }

    @Test
    @DisplayName("CLOSED can transition to IN_PROGRESS and CANCELLED only")
    void closedTransitions() {
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.OPEN));
        assertFalse(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.COMPLETED));
        assertFalse(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.CLOSED));
        assertFalse(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.DRAFT));
    }

    @Test
    @DisplayName("IN_PROGRESS can transition to COMPLETED and CANCELLED only")
    void inProgressTransitions() {
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.COMPLETED));
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.OPEN));
        assertFalse(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.CLOSED));
        assertFalse(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertFalse(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.DRAFT));
    }

    @Test
    @DisplayName("COMPLETED and CANCELLED cannot transition to any status")
    void terminalTransitions() {
        assertFalse(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.OPEN));
        assertFalse(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.DRAFT));
        assertFalse(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.OPEN));
        assertFalse(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.DRAFT));
    }
}
