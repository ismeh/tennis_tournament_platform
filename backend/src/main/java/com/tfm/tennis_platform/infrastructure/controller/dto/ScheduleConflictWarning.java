package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.List;
import java.util.UUID;

public record ScheduleConflictWarning(
        UUID matchId,
        List<String> warnings,
        boolean saved
) {
    public static ScheduleConflictWarning saved(UUID matchId, List<String> warnings) {
        return new ScheduleConflictWarning(matchId, warnings, true);
    }

    public static ScheduleConflictWarning withWarnings(UUID matchId, List<String> warnings) {
        return new ScheduleConflictWarning(matchId, warnings, true);
    }
}
