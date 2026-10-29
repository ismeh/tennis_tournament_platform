package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.MatchStatus;

import java.util.UUID;

public record MatchResultRequest(
        UUID winnerId,
        java.util.List<SetScoreRequest> sets,
        String notes,
        String firstPlayerPoints,
        String secondPlayerPoints,
        MatchStatus status
) {}