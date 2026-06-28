package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Match {
    private final UUID id;
    private final Tournament tournament;
    private final UUID drawId;
    private final Inscription firstInscription;
    private final Inscription secondInscription;
    private final Inscription winner;
    private final Integer roundNumber;
    private final Integer bracketPosition;
    private final Match nextMatch;
    private final Match loserNextMatch;
    private final LocalDateTime scheduledAt;
    private final ScheduleTimeType scheduleTimeType;
    private final UUID courtId;
    private final String court;
    private final String result;
    private final MatchStatus status;

    public UUID getTournamentId() {
        return tournament != null ? tournament.getId() : null;
    }

    public UUID getFirstInscriptionId() {
        return firstInscription != null ? firstInscription.getId() : null;
    }

    public UUID getSecondInscriptionId() {
        return secondInscription != null ? secondInscription.getId() : null;
    }

    public UUID getWinnerId() {
        return winner != null ? winner.getId() : null;
    }

    public UUID getLoserNextMatchId() {
        return loserNextMatch != null ? loserNextMatch.getId() : null;
    }

    public boolean isProfessionalMatch() {
        return firstInscription != null
                && secondInscription != null
                && firstInscription.isProfessional()
                && secondInscription.isProfessional();
    }

    public Integer getFirstWinPoints() {
        return isProfessionalMatch() ? secondInscription.getProfessionalAwardedPoints() : null;
    }

    public Integer getSecondWinPoints() {
        return isProfessionalMatch() ? firstInscription.getProfessionalAwardedPoints() : null;
    }
}
