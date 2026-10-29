package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record ReorganizeMatchesRequest(
    UUID matchId1,
    String slot1,
    UUID matchId2,
    String slot2
) {}
