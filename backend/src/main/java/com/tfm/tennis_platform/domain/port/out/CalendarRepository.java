package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CalendarRepository {
    List<TournamentCalendarItem> findPublishedTournaments(
            LocalDate from,
            LocalDate to,
            List<TournamentStatus> statuses,
            Surface surface,
            String location,
            String name,
            Boolean professionalTournament,
            String requesterEmail
    );

    List<PlayerMatchCalendarItem> findScheduledMatchesForPlayer(
            String playerEmail,
            LocalDateTime from,
            LocalDateTime to,
            List<TournamentStatus> statuses
    );

    List<TournamentCalendarItem> findMyTournaments(
            String organizerEmail,
            LocalDate from,
            LocalDate to
    );
}
