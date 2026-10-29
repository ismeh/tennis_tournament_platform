package com.puntomatch.infrastructure.controller.dto;

import java.util.List;
import java.util.UUID;

public record TournamentEventResponse(
    UUID eventId,
    Integer categoryId,
    String gender,
    List<StageResponse> stages
) {}
