package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record CourtResponse(
    UUID id,
    UUID tournamentId,
    String name,
    boolean active
) {}
