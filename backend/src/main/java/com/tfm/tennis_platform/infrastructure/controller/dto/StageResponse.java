package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.StageType;

import java.util.List;
import java.util.UUID;

public record StageResponse(
    UUID id,
    UUID eventId,
    StageType stageType,
    Integer order,
    String strategyName,
    String description,
    List<DrawResponse> draws
) {}
