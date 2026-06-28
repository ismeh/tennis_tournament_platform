package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AccountExportResponse(
        AccountInfo account,
        PersonInfo person,
        List<ConsentInfo> consentHistory,
        List<ParticipantSummary> participations
) {
    public record AccountInfo(
            String email,
            String role,
            String tier,
            LocalDateTime registeredAt,
            boolean privacyPolicyAccepted,
            String privacyPolicyVersion,
            boolean termsConditionsAccepted,
            String termsConditionsVersion
    ) {
    }

    public record PersonInfo(
            String firstName,
            String lastName,
            String nationality,
            LocalDate birthDate,
            String gender,
            String tennisId
    ) {
    }

    public record ConsentInfo(
            String documentType,
            String action,
            LocalDateTime createdAt
    ) {
    }

    public record TournamentSummary(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
    }

    public record ParticipantSummary(
            String tournamentName,
            String eventName,
            String entryStatus
    ) {
    }
}
