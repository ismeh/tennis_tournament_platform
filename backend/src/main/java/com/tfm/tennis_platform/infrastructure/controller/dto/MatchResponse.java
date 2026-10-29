package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID firstInscriptionId,
    UUID secondInscriptionId,
    UUID winnerId,
    Integer roundNumber,
    Integer bracketPosition,
    LocalDateTime scheduledAt,
    String scheduleTimeType,
    UUID courtId,
    String court,
    String result,
    java.util.List<SetScoreResponse> sets,
    String notes,
    Boolean professionalMatch,
    Integer firstWinPoints,
    Integer secondWinPoints,
    String firstPlayerPoints,
    String secondPlayerPoints,
    String status
) {
    public MatchResponse(
        UUID id,
        UUID firstInscriptionId,
        UUID secondInscriptionId,
        UUID winnerId,
        Integer roundNumber,
        Integer bracketPosition,
        LocalDateTime scheduledAt,
        String scheduleTimeType,
        UUID courtId,
        String court,
        String result,
        Boolean professionalMatch,
        Integer firstWinPoints,
        Integer secondWinPoints,
        String status
    ) {
        this(id, firstInscriptionId, secondInscriptionId, winnerId, roundNumber, bracketPosition,
             scheduledAt, scheduleTimeType, courtId, court, result, java.util.Collections.emptyList(), null,
             professionalMatch, firstWinPoints, secondWinPoints, null, null, status);
    }

    public MatchResponse(
        UUID id,
        UUID firstInscriptionId,
        UUID secondInscriptionId,
        UUID winnerId,
        Integer roundNumber,
        Integer bracketPosition,
        LocalDateTime scheduledAt,
        String scheduleTimeType,
        UUID courtId,
        String court,
        String result,
        java.util.List<SetScoreResponse> sets,
        String notes,
        Boolean professionalMatch,
        Integer firstWinPoints,
        Integer secondWinPoints,
        String status
    ) {
        this(id, firstInscriptionId, secondInscriptionId, winnerId, roundNumber, bracketPosition,
             scheduledAt, scheduleTimeType, courtId, court, result, sets, notes,
             professionalMatch, firstWinPoints, secondWinPoints, null, null, status);
    }
}
