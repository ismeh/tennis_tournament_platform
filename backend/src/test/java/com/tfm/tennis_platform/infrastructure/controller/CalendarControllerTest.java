package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.CalendarService;
import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CalendarRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarController")
class CalendarControllerTest {

    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private CalendarController controller;

    @Test
    @DisplayName("getPublishedTournaments returns mapped page with principal")
    void shouldReturnPublishedTournamentsWithPrincipal() {
        Principal principal = () -> "user@test.com";
        TournamentCalendarItem item = new TournamentCalendarItem(
                UUID.randomUUID(), "Tournament 1",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                LocalTime.of(9, 0), "Madrid", Surface.CLAY, 32,
                TournamentStatus.OPEN, true);

        CalendarRepository.PageResult<TournamentCalendarItem> result =
                new CalendarRepository.PageResult<>(List.of(item), 1);

        when(calendarService.findPublishedTournamentsPaginated(
                eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 5)),
                eq(Surface.CLAY), eq("Madrid"), eq("Tournament"), eq(true),
                eq(TournamentStatus.OPEN), eq("user@test.com"), eq(0), eq(10)))
                .thenReturn(result);

        var response = controller.getPublishedTournaments(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                Surface.CLAY, "Madrid", "Tournament", true,
                TournamentStatus.OPEN, 0, 10, principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().page()).isEqualTo(0);
        assertThat(response.getBody().size()).isEqualTo(10);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getPublishedTournaments handles null principal")
    void shouldHandleNullPrincipal() {
        CalendarRepository.PageResult<TournamentCalendarItem> result =
                new CalendarRepository.PageResult<>(List.of(), 0);

        when(calendarService.findPublishedTournamentsPaginated(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(0), eq(10)))
                .thenReturn(result);

        var response = controller.getPublishedTournaments(
                null, null, null, null, null, null, null, 0, 10, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("getPublishedTournaments calculates totalPages correctly")
    void shouldCalculateTotalPages() {
        CalendarRepository.PageResult<TournamentCalendarItem> result =
                new CalendarRepository.PageResult<>(List.of(), 25);

        when(calendarService.findPublishedTournamentsPaginated(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(result);

        var response = controller.getPublishedTournaments(
                null, null, null, null, null, null, null, 0, 10, null);

        assertThat(response.getBody().totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("getPublishedTournaments maps scheduleTimeType to string")
    void shouldMapScheduleTimeType() {
        TournamentCalendarItem item = new TournamentCalendarItem(
                UUID.randomUUID(), "T",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                LocalTime.of(9, 0), "M", Surface.HARD, 16,
                TournamentStatus.OPEN, false);

        CalendarRepository.PageResult<TournamentCalendarItem> result =
                new CalendarRepository.PageResult<>(List.of(item), 1);

        when(calendarService.findPublishedTournamentsPaginated(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(result);

        var response = controller.getPublishedTournaments(
                null, null, null, null, null, null, null, 0, 10, null);

        assertThat(response.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("getPublishedTournaments with empty result returns empty content")
    void shouldReturnEmptyContent() {
        CalendarRepository.PageResult<TournamentCalendarItem> result =
                new CalendarRepository.PageResult<>(List.of(), 0);

        when(calendarService.findPublishedTournamentsPaginated(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(result);

        var response = controller.getPublishedTournaments(
                null, null, null, null, null, null, null, 0, 10, null);

        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    @DisplayName("getMyMatches returns mapped match list")
    void shouldReturnMyMatches() {
        Principal principal = () -> "player@test.com";
        PlayerMatchCalendarItem item = new PlayerMatchCalendarItem(
                UUID.randomUUID(), "Tournament 1",
                UUID.randomUUID(), "Event 1",
                UUID.randomUUID(), 2,
                LocalDateTime.of(2026, 7, 3, 14, 0),
                ScheduleTimeType.EXACT,
                UUID.randomUUID(), "Court 1",
                UUID.randomUUID(), "Player A",
                UUID.randomUUID(), "Player B",
                "6-3 7-5");

        when(calendarService.findScheduledMatchesForPlayer(
                eq("player@test.com"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 5))))
                .thenReturn(List.of(item));

        var response = controller.getMyMatches(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getMyMatches maps null scheduleTimeType to null string")
    void shouldMapNullScheduleTimeTypeToNull() {
        Principal principal = () -> "player@test.com";
        PlayerMatchCalendarItem item = new PlayerMatchCalendarItem(
                UUID.randomUUID(), "T",
                UUID.randomUUID(), "E",
                UUID.randomUUID(), 1,
                LocalDateTime.of(2026, 7, 3, 14, 0),
                null,
                UUID.randomUUID(), "Court",
                UUID.randomUUID(), "A",
                UUID.randomUUID(), "B",
                null);

        when(calendarService.findScheduledMatchesForPlayer(
                eq("player@test.com"), eq(null), eq(null)))
                .thenReturn(List.of(item));

        var response = controller.getMyMatches(null, null, principal);

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getMyMatches returns empty list when no matches")
    void shouldReturnEmptyMyMatches() {
        Principal principal = () -> "player@test.com";
        when(calendarService.findScheduledMatchesForPlayer(
                eq("player@test.com"), any(), any()))
                .thenReturn(List.of());

        var response = controller.getMyMatches(null, null, principal);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("getMyTournaments returns mapped tournament list")
    void shouldReturnMyTournaments() {
        Principal principal = () -> "organizer@test.com";
        TournamentCalendarItem item = new TournamentCalendarItem(
                UUID.randomUUID(), "My Tournament",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                LocalTime.of(9, 0), "Valencia", Surface.CLAY, 32,
                TournamentStatus.OPEN, true);

        when(calendarService.findMyTournaments(
                eq("organizer@test.com"), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 5))))
                .thenReturn(List.of(item));

        var response = controller.getMyTournaments(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getMyTournaments returns empty when none found")
    void shouldReturnEmptyMyTournaments() {
        Principal principal = () -> "organizer@test.com";
        when(calendarService.findMyTournaments(
                eq("organizer@test.com"), eq(null), eq(null)))
                .thenReturn(List.of());

        var response = controller.getMyTournaments(null, null, principal);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("getMyTournaments with multiple items maps all")
    void shouldMapMultipleMyTournaments() {
        Principal principal = () -> "org@test.com";
        List<TournamentCalendarItem> items = List.of(
                new TournamentCalendarItem(UUID.randomUUID(), "T1",
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3),
                        LocalTime.of(9, 0), "M", Surface.CLAY, 16, TournamentStatus.OPEN, false),
                new TournamentCalendarItem(UUID.randomUUID(), "T2",
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                        LocalTime.of(10, 0), "B", Surface.HARD, 32, TournamentStatus.CLOSED, true)
        );

        when(calendarService.findMyTournaments(
                eq("org@test.com"), any(), any()))
                .thenReturn(items);

        var response = controller.getMyTournaments(null, null, principal);

        assertThat(response.getBody()).hasSize(2);
    }
}
