package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PlayerInscriptionResponse(
    UUID tournamentId,
    String tournamentName,
    UUID eventId,
    String eventName,
    String categoryName,
    String entryStatus,
    String paymentStatus,
    LocalDate playStartDate,
    LocalDate playEndDate
) {}
