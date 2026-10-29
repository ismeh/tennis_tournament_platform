package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.List;

public record ScheduleMatchResponse(
        MatchResponse match,
        List<String> warnings
) {
    public static ScheduleMatchResponse of(MatchResponse match, List<String> warnings) {
        return new ScheduleMatchResponse(match, warnings);
    }

    public static ScheduleMatchResponse of(MatchResponse match) {
        return new ScheduleMatchResponse(match, List.of());
    }
}
