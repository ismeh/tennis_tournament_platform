package com.tfm.tennis_platform.domain.models.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TournamentStatus")
class TournamentStatusTest {

    @ParameterizedTest
    @EnumSource(TournamentStatus.class)
    @DisplayName("Any status can transition to any other status")
    void anyTransitionAllowed(TournamentStatus from) {
        for (TournamentStatus to : TournamentStatus.values()) {
            if (from == to) {
                assertFalse(from.canTransitionTo(to),
                        from + " should NOT self-transition to " + to);
            } else {
                assertTrue(from.canTransitionTo(to),
                        from + " should transition to " + to);
            }
        }
    }

    @Test
    @DisplayName("DRAFT can transition to OPEN, CLOSED, IN_PROGRESS, COMPLETED, CANCELLED")
    void draftTransitions() {
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.OPEN));
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.CLOSED));
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.COMPLETED));
        assertTrue(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.DRAFT.canTransitionTo(TournamentStatus.DRAFT));
    }

    @Test
    @DisplayName("OPEN can transition to DRAFT, CLOSED, IN_PROGRESS, COMPLETED, CANCELLED")
    void openTransitions() {
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.DRAFT));
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.CLOSED));
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.COMPLETED));
        assertTrue(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.OPEN.canTransitionTo(TournamentStatus.OPEN));
    }

    @Test
    @DisplayName("CLOSED can transition to DRAFT, OPEN, IN_PROGRESS, COMPLETED, CANCELLED")
    void closedTransitions() {
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.DRAFT));
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.OPEN));
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.COMPLETED));
        assertTrue(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.CLOSED.canTransitionTo(TournamentStatus.CLOSED));
    }

    @Test
    @DisplayName("IN_PROGRESS can transition to DRAFT, OPEN, CLOSED, COMPLETED, CANCELLED")
    void inProgressTransitions() {
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.DRAFT));
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.OPEN));
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.CLOSED));
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.COMPLETED));
        assertTrue(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.IN_PROGRESS.canTransitionTo(TournamentStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("COMPLETED can transition to any status except itself")
    void completedTransitions() {
        assertTrue(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.DRAFT));
        assertTrue(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.OPEN));
        assertTrue(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.CLOSED));
        assertTrue(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertTrue(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.CANCELLED));
        assertFalse(TournamentStatus.COMPLETED.canTransitionTo(TournamentStatus.COMPLETED));
    }

    @Test
    @DisplayName("CANCELLED can transition to any status except itself")
    void cancelledTransitions() {
        assertTrue(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.DRAFT));
        assertTrue(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.OPEN));
        assertTrue(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.CLOSED));
        assertTrue(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.IN_PROGRESS));
        assertTrue(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.COMPLETED));
        assertFalse(TournamentStatus.CANCELLED.canTransitionTo(TournamentStatus.CANCELLED));
    }
}
