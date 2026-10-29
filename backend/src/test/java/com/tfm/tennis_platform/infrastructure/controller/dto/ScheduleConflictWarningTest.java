package com.tfm.tennis_platform.infrastructure.controller.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleConflictWarningTest {

    @Test
    void savedFactoryCreatesWarningWithSavedTrue() {
        UUID matchId = UUID.randomUUID();
        List<String> warnings = List.of("Court busy");

        ScheduleConflictWarning warning = ScheduleConflictWarning.saved(matchId, warnings);

        assertThat(warning.matchId()).isEqualTo(matchId);
        assertThat(warning.warnings()).containsExactly("Court busy");
        assertThat(warning.saved()).isTrue();
    }

    @Test
    void withWarningsFactoryCreatesWarningWithSavedTrue() {
        UUID matchId = UUID.randomUUID();
        List<String> warnings = List.of("Player conflict", "Outside play period");

        ScheduleConflictWarning warning = ScheduleConflictWarning.withWarnings(matchId, warnings);

        assertThat(warning.matchId()).isEqualTo(matchId);
        assertThat(warning.warnings()).hasSize(2);
        assertThat(warning.saved()).isTrue();
    }

    @Test
    void emptyWarningsList() {
        UUID matchId = UUID.randomUUID();

        ScheduleConflictWarning warning = ScheduleConflictWarning.saved(matchId, List.of());

        assertThat(warning.warnings()).isEmpty();
        assertThat(warning.saved()).isTrue();
    }
}
