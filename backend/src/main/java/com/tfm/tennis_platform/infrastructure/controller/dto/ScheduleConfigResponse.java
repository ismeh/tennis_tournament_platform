package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ScheduleConfigResponse(
    UUID id,
    UUID tournamentId,
    List<TimeSlotResponse> timeSlots,
    int matchDurationMinutes
) {
    public record TimeSlotResponse(
        LocalTime startTime,
        LocalTime endTime
    ) {}
}
