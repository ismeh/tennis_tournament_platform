package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.DrawType;

import java.util.UUID;

public record DrawResponse(
    UUID drawId,
    DrawType drawType,
    String drawName
) {}
