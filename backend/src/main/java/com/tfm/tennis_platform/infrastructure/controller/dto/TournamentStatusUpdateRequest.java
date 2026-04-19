package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

public record TournamentStatusUpdateRequest(
    TournamentStatus status
) {}
