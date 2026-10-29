package com.puntomatch.infrastructure.controller.dto;

import com.puntomatch.domain.models.enums.TournamentStatus;

public record TournamentStatusUpdateRequest(
    TournamentStatus status
) {}
