package com.tfm.tennis_platform.domain.models.club;

import java.util.UUID;

public record ClubRankingEntry(
        Integer position,
        UUID personId,
        String firstName,
        String lastName,
        String gender,
        String license,
        Long points,
        Long victories,
        Long tournaments
) {
    public ClubRankingEntry withPosition(Integer newPosition) {
        return new ClubRankingEntry(newPosition, personId, firstName, lastName, gender, license, points, victories, tournaments);
    }
}
