package com.tfm.tennis_platform.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record TournamentUpdateEvent(
        TournamentUpdateType type,
        UUID tournamentId,
        UUID matchId,
        LocalDateTime occurredAt
) {

    public static TournamentUpdateEvent matchResultUpdated(UUID tournamentId, UUID matchId) {
        return new TournamentUpdateEvent(TournamentUpdateType.MATCH_RESULT_UPDATED, tournamentId, matchId, LocalDateTime.now());
    }

    public static TournamentUpdateEvent matchScheduleUpdated(UUID tournamentId, UUID matchId) {
        return new TournamentUpdateEvent(TournamentUpdateType.MATCH_SCHEDULE_UPDATED, tournamentId, matchId, LocalDateTime.now());
    }
}
