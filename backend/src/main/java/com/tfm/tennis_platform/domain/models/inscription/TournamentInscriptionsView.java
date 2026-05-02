package com.tfm.tennis_platform.domain.models.inscription;

import java.util.List;
import java.util.UUID;

public record TournamentInscriptionsView(
        UUID tournamentId,
        UUID selectedEventId,
        List<TournamentInscriptionEventView> events,
        List<TournamentInscriptionCategoryCount> categoryCounts,
        List<TournamentInscriptionPlayerView> inscriptions
) {
}
