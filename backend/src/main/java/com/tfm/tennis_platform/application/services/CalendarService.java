package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final int DEFAULT_WINDOW_DAYS = 90;
    private static final List<TournamentStatus> PUBLIC_CALENDAR_STATUSES = List.of(
            TournamentStatus.OPEN,
            TournamentStatus.CLOSED,
            TournamentStatus.IN_PROGRESS
    );

    private final CalendarRepository calendarRepository;

    public List<TournamentCalendarItem> findPublishedTournaments(
            LocalDate from,
            LocalDate to,
            Surface surface,
            String location
    ) {
        LocalDate startDate = resolveStartDate(from);
        LocalDate endDate = resolveEndDate(startDate, to);
        validateDateRange(startDate, endDate);

        return calendarRepository.findPublishedTournaments(
                startDate,
                endDate,
                PUBLIC_CALENDAR_STATUSES,
                surface,
                normalizeFilter(location)
        );
    }

    public List<PlayerMatchCalendarItem> findScheduledMatchesForPlayer(String playerEmail, LocalDate from, LocalDate to) {
        if (playerEmail == null || playerEmail.isBlank()) {
            throw new InvalidArgumentException("Inicia sesión para consultar tus partidos.");
        }

        LocalDate startDate = resolveStartDate(from);
        LocalDate endDate = resolveEndDate(startDate, to);
        validateDateRange(startDate, endDate);

        return calendarRepository.findScheduledMatchesForPlayer(
                playerEmail,
                startDate.atStartOfDay(),
                LocalDateTime.of(endDate, LocalTime.MAX),
                PUBLIC_CALENDAR_STATUSES
        );
    }

    private LocalDate resolveStartDate(LocalDate from) {
        return from != null ? from : LocalDate.now();
    }

    private LocalDate resolveEndDate(LocalDate startDate, LocalDate to) {
        return to != null ? to : startDate.plusDays(DEFAULT_WINDOW_DAYS);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new InvalidArgumentException("La fecha final debe ser posterior a la fecha inicial.");
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase();
    }
}
