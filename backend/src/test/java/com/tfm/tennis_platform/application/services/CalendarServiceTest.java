package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarRepository calendarRepository;

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(calendarRepository);
    }

    @Test
    void should_find_published_tournaments_with_public_statuses_and_filters() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        TournamentCalendarItem item = new TournamentCalendarItem(
                UUID.randomUUID(),
                "Open de Verano",
                from,
                from.plusDays(5),
                LocalTime.of(9, 0),
                "Club Central",
                Surface.CLAY,
                32,
                TournamentStatus.OPEN,
                false
        );

        when(calendarRepository.findPublishedTournaments(
                from,
                to,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                Surface.CLAY,
                "central",
                null,
                null,
                null
        )).thenReturn(List.of(item));

        List<TournamentCalendarItem> result = calendarService.findPublishedTournaments(from, to, Surface.CLAY, " Central ", null, null, null, null);

        assertEquals(List.of(item), result);
    }

    @Test
    void should_find_player_matches_for_full_requested_days() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);
        PlayerMatchCalendarItem item = new PlayerMatchCalendarItem(
                UUID.randomUUID(),
                "Open de Verano",
                UUID.randomUUID(),
                "Absoluto - Masculino",
                UUID.randomUUID(),
                1,
                LocalDateTime.of(2026, 6, 2, 20, 0),
                null,
                null,
                "Pista 1",
                UUID.randomUUID(),
                "Carlos Lopez",
                UUID.randomUUID(),
                "Luis Garcia",
                null
        );

        when(calendarRepository.findScheduledMatchesForPlayer(
                "player@example.com",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(to, LocalTime.MAX),
                List.of(TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED)
        )).thenReturn(List.of(item));

        List<PlayerMatchCalendarItem> result = calendarService.findScheduledMatchesForPlayer("player@example.com", from, to);

        assertEquals(List.of(item), result);
    }

    @Test
    void should_reject_invalid_date_range() {
        assertThrows(
                InvalidArgumentException.class,
                () -> calendarService.findPublishedTournaments(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 6, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void should_reject_player_matches_without_email() {
        assertThrows(
                InvalidArgumentException.class,
                () -> calendarService.findScheduledMatchesForPlayer(" ", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2))
        );
    }
}
