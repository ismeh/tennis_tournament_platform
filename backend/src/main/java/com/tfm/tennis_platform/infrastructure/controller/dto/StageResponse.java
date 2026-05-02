package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.StageType;

import java.util.List;
import java.util.UUID;

public record StageResponse(
    UUID stageId,
    Integer stageNumber,
    StageType stageType,
    List<DrawResponse> draws
) {}
