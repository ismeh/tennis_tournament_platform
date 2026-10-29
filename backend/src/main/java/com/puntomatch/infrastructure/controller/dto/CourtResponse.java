package com.puntomatch.infrastructure.controller.dto;

import java.util.UUID;

public record CourtResponse(
    UUID id,
    UUID tournamentId,
    String name,
    boolean active
) {}
