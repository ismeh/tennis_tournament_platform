package com.puntomatch.infrastructure.controller.dto;

import com.puntomatch.domain.models.enums.StageType;

import java.util.List;
import java.util.UUID;

public record StageResponse(
    UUID id,
    UUID eventId,
    StageType stageType,
    Integer order,
    String description,
    List<DrawResponse> draws
) {}
