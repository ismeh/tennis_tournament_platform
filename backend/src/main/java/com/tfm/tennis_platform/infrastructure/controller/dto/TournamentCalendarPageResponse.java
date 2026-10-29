package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TournamentCalendarPageResponse(
        List<TournamentCalendarResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
