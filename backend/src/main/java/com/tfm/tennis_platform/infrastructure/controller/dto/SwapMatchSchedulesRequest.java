package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record SwapMatchSchedulesRequest(
    UUID matchId1,
    UUID matchId2
) {}
