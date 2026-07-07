package com.tfm.tennis_platform.infrastructure.controller.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleMatchResponseTest {

    @Test
    void ofWithWarningsCreatesResponse() {
        MatchResponse match = new MatchResponse(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        List<String> warnings = List.of("Court busy");

        ScheduleMatchResponse response = ScheduleMatchResponse.of(match, warnings);

        assertThat(response.match()).isEqualTo(match);
        assertThat(response.warnings()).containsExactly("Court busy");
    }

    @Test
    void ofWithoutWarningsCreatesEmptyWarningsList() {
        MatchResponse match = new MatchResponse(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        ScheduleMatchResponse response = ScheduleMatchResponse.of(match);

        assertThat(response.match()).isEqualTo(match);
        assertThat(response.warnings()).isEmpty();
    }
}
