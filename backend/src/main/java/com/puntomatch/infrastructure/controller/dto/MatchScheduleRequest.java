package com.puntomatch.infrastructure.controller.dto;

import com.puntomatch.domain.models.enums.ScheduleTimeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchScheduleRequest(
    UUID courtId,
    LocalDateTime scheduledAt,
    ScheduleTimeType scheduleTimeType
) {}
