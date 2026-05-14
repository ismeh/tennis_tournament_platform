package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.DrawType;

import java.util.List;
import java.util.UUID;

public record DrawResponse(
    UUID id,
    UUID stageId,
    DrawType drawType,
    String label,
    List<MatchResponse> matches
) {}
