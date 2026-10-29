package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.models.calendar.PlayerInscriptionItem;
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
    private static final List<TournamentStatus> VISIBLE_CALENDAR_STATUSES = List.of(
            TournamentStatus.DRAFT,
            TournamentStatus.OPEN,
            TournamentStatus.CLOSED,
            TournamentStatus.IN_PROGRESS,
            TournamentStatus.COMPLETED,
            TournamentStatus.CANCELLED
    );

    private final CalendarRepository calendarRepository;

    public CalendarRepository.PageResult<TournamentCalendarItem> findPublishedTournamentsPaginated(
            LocalDate from,
            LocalDate to,
            Surface surface,
            String location,
            String name,
            Boolean professionalTournament,
            TournamentStatus status,
            String requesterEmail,
            int page,
            int size
    ) {
        LocalDate startDate = resolveStartDate(from);
        LocalDate endDate = resolveEndDate(startDate, to);
        validateDateRange(startDate, endDate);

        return calendarRepository.findPublishedTournamentsPaginated(
                startDate,
                endDate,
                status != null ? List.of(status) : VISIBLE_CALENDAR_STATUSES,
                surface,
                normalizeFilter(location),
                normalizeFilter(name),
                professionalTournament,
                normalizeFilter(requesterEmail),
                page,
                size
        );
    }

    public List<TournamentCalendarItem> findPublishedTournaments(
            LocalDate from,
            LocalDate to,
            Surface surface,
            String location,
            String name,
            Boolean professionalTournament,
            TournamentStatus status,
            String requesterEmail
    ) {
        LocalDate startDate = resolveStartDate(from);
        LocalDate endDate = resolveEndDate(startDate, to);
        validateDateRange(startDate, endDate);

        return calendarRepository.findPublishedTournaments(
                startDate,
                endDate,
                status != null ? List.of(status) : VISIBLE_CALENDAR_STATUSES,
                surface,
                normalizeFilter(location),
                normalizeFilter(name),
                professionalTournament,
                normalizeFilter(requesterEmail)
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
                VISIBLE_CALENDAR_STATUSES.stream()
                        .filter(status -> status != TournamentStatus.DRAFT)
                        .toList()
        );
    }

    public List<TournamentCalendarItem> findMyTournaments(String organizerEmail, LocalDate from, LocalDate to) {
        if (organizerEmail == null || organizerEmail.isBlank()) {
            throw new InvalidArgumentException("Inicia sesión para consultar tus torneos.");
        }

        LocalDate startDate = from != null ? from : null;
        LocalDate endDate = to != null ? to : null;

        if (startDate != null && endDate != null) {
            validateDateRange(startDate, endDate);
        }

        return calendarRepository.findMyTournaments(organizerEmail, startDate, endDate);
    }

    public List<PlayerInscriptionItem> findMyInscriptions(String playerEmail) {
        if (playerEmail == null || playerEmail.isBlank()) {
            throw new InvalidArgumentException("Inicia sesión para consultar tus inscripciones.");
        }
        return calendarRepository.findMyInscriptions(playerEmail);
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
