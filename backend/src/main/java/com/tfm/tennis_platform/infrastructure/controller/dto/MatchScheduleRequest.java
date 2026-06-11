package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchScheduleRequest(
    UUID courtId,
    LocalDateTime scheduledAt,
    ScheduleTimeType scheduleTimeType,
    Boolean cascade
) {}
