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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void should_reject_player_matches_with_null_email() {
        assertThrows(
                InvalidArgumentException.class,
                () -> calendarService.findScheduledMatchesForPlayer(null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2))
        );
    }

    @Test
    void should_filter_by_specific_status() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findPublishedTournaments(
                from, to,
                List.of(TournamentStatus.OPEN),
                null, null, null, null, null
        )).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findPublishedTournaments(from, to, null, null, null, null, TournamentStatus.OPEN, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_handle_paginated_published_tournaments() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findPublishedTournamentsPaginated(
                from, to,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                null, null, null, null, null, 0, 10
        )).thenReturn(new CalendarRepository.PageResult<>(List.of(), 0));

        calendarService.findPublishedTournamentsPaginated(from, to, null, null, null, null, null, null, 0, 10);
    }

    @Test
    void should_handle_null_date_defaults_in_published_tournaments() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findPublishedTournaments(
                from, to,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                null, null, null, null, null
        )).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findPublishedTournaments(from, to, null, null, null, null, null, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_find_my_tournaments() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findMyTournaments("admin@test.com", from, to)).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findMyTournaments("admin@test.com", from, to);

        assertEquals(List.of(), result);
    }

    @Test
    void should_reject_my_tournaments_with_null_email() {
        assertThrows(
                InvalidArgumentException.class,
                () -> calendarService.findMyTournaments(null, null, null)
        );
    }

    @Test
    void should_reject_my_tournaments_with_blank_email() {
        assertThrows(
                InvalidArgumentException.class,
                () -> calendarService.findMyTournaments("  ", null, null)
        );
    }

    @Test
    void should_find_my_tournaments_with_null_dates() {
        when(calendarRepository.findMyTournaments("admin@test.com", null, null)).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findMyTournaments("admin@test.com", null, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_handle_null_start_date_defaults_to_now() {
        LocalDate to = LocalDate.now().plusDays(30);
        when(calendarRepository.findPublishedTournaments(
                LocalDate.now(), to,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                null, null, null, null, null
        )).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findPublishedTournaments(null, to, null, null, null, null, null, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_handle_null_end_date_defaults_to_start_plus_90_days() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate expectedTo = from.plusDays(90);
        when(calendarRepository.findPublishedTournaments(
                from, expectedTo,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                null, null, null, null, null
        )).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findPublishedTournaments(from, null, null, null, null, null, null, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_find_player_matches_with_null_dates() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(90);
        when(calendarRepository.findScheduledMatchesForPlayer(
                "player@example.com",
                from.atStartOfDay(),
                LocalDateTime.of(to, LocalTime.MAX),
                List.of(TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED)
        )).thenReturn(List.of());

        List<PlayerMatchCalendarItem> result = calendarService.findScheduledMatchesForPlayer("player@example.com", null, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_handle_paginated_with_null_dates() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(90);
        when(calendarRepository.findPublishedTournamentsPaginated(
                from, to,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                Surface.CLAY, null, null, null, null, 0, 10
        )).thenReturn(new CalendarRepository.PageResult<>(List.of(), 0));

        calendarService.findPublishedTournamentsPaginated(null, null, Surface.CLAY, null, null, null, null, null, 0, 10);
    }

    @Test
    void should_handle_paginated_with_specific_status() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findPublishedTournamentsPaginated(
                from, to,
                List.of(TournamentStatus.OPEN),
                null, null, null, null, null, 0, 10
        )).thenReturn(new CalendarRepository.PageResult<>(List.of(), 0));

        calendarService.findPublishedTournamentsPaginated(from, to, null, null, null, null, TournamentStatus.OPEN, null, 0, 10);
    }

    @Test
    void should_find_player_matches_with_only_from_date() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = from.plusDays(90);
        when(calendarRepository.findScheduledMatchesForPlayer(
                "player@example.com",
                from.atStartOfDay(),
                LocalDateTime.of(to, LocalTime.MAX),
                List.of(TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED)
        )).thenReturn(List.of());

        List<PlayerMatchCalendarItem> result = calendarService.findScheduledMatchesForPlayer("player@example.com", from, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_find_published_with_name_filter() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findPublishedTournaments(
                from, to,
                List.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED, TournamentStatus.CANCELLED),
                null, null, "open", null, null
        )).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findPublishedTournaments(from, to, null, null, "Open", null, null, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_find_my_tournaments_with_only_from_date() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        when(calendarRepository.findMyTournaments("admin@test.com", from, null)).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findMyTournaments("admin@test.com", from, null);

        assertEquals(List.of(), result);
    }

    @Test
    void should_find_my_tournaments_with_only_to_date() {
        LocalDate to = LocalDate.of(2026, 7, 1);
        when(calendarRepository.findMyTournaments("admin@test.com", null, to)).thenReturn(List.of());

        List<TournamentCalendarItem> result = calendarService.findMyTournaments("admin@test.com", null, to);

        assertEquals(List.of(), result);
    }
}
