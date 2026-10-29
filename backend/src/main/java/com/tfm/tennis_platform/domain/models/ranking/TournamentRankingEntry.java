package com.tfm.tennis_platform.domain.models.ranking;

import java.util.UUID;

public record TournamentRankingEntry(
        Integer position,
        UUID participantId,
        String license,
        String firstName,
        String lastName,
        String gender,
        Long points,
        Long victories
) {
    public TournamentRankingEntry withPosition(Integer newPosition) {
        return new TournamentRankingEntry(newPosition, participantId, license, firstName, lastName, gender, points, victories);
    }
}
