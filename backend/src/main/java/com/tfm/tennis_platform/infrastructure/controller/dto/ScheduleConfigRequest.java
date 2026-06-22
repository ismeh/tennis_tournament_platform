package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalTime;
import java.util.List;

public record ScheduleConfigRequest(
    List<TimeSlotRequest> timeSlots,
    int matchDurationMinutes
) {
    public record TimeSlotRequest(
        LocalTime startTime,
        LocalTime endTime
    ) {}
}
