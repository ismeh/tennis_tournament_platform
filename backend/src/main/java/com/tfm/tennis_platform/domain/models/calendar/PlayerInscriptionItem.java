package com.tfm.tennis_platform.domain.models.calendar;

import java.time.LocalDate;
import java.util.UUID;

public record PlayerInscriptionItem(
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
