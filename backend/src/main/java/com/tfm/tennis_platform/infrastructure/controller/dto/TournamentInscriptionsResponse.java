package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.List;
import java.util.UUID;

public record TournamentInscriptionsResponse(
        UUID tournamentId,
        UUID selectedEventId,
        List<TournamentInscriptionEventResponse> events,
        List<TournamentInscriptionCategoryCountResponse> categoryCounts,
        List<TournamentInscriptionPlayerResponse> inscriptions
) {
}
