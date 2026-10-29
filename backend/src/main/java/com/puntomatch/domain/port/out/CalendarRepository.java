package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.calendar.PlayerMatchCalendarItem;
import com.puntomatch.domain.models.calendar.TournamentCalendarItem;
import com.puntomatch.domain.models.enums.Surface;
import com.puntomatch.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CalendarRepository {
    List<TournamentCalendarItem> findPublishedTournaments(
            LocalDate from,
            LocalDate to,
            List<TournamentStatus> statuses,
            Surface surface,
            String location
    );

    List<PlayerMatchCalendarItem> findScheduledMatchesForPlayer(
            String playerEmail,
            LocalDateTime from,
            LocalDateTime to,
            List<TournamentStatus> statuses
    );
}
