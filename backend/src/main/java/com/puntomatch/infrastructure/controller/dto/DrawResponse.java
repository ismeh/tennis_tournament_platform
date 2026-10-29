package com.puntomatch.infrastructure.controller.dto;

import com.puntomatch.domain.models.enums.DrawType;

import java.util.List;
import java.util.UUID;

public record DrawResponse(
    UUID id,
    UUID stageId,
    DrawType drawType,
    String label,
    List<MatchResponse> matches
) {}
