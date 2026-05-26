package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record MatchResultRequest(
        UUID winnerId,
        String scoreString
) {}