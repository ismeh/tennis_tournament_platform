package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.RankingService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.ranking.ProfessionalRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingController")
class RankingControllerTest {

    @Mock
    private RankingService rankingService;
    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private RankingController controller;

    @Test
    @DisplayName("getProfessionalRanking returns mapped page response")
    void shouldReturnProfessionalRankingPage() {
        ProfessionalRankingEntry entry = new ProfessionalRankingEntry(
                1, 100, "LIC-001", "John Doe", "John", "Doe", "MALE", "A", "Club", LocalDate.of(1990, 1, 1), 1500);
        RankingPage<ProfessionalRankingEntry> page = new RankingPage<>(
                List.of(entry), 0, 10, 1, 1, "position", "asc");

        when(rankingService.findProfessionalRanking("MALE", "A", 0, 10, "position", "asc"))
                .thenReturn(page);

        ResponseEntity<?> response = controller.getProfessionalRanking("MALE", "A", 0, 10, "position", "asc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getProfessionalRanking with null params uses defaults")
    void shouldReturnProfessionalRankingWithNullParams() {
        RankingPage<ProfessionalRankingEntry> emptyPage = new RankingPage<>(
                List.of(), 0, 10, 0, 0, "position", "asc");

        when(rankingService.findProfessionalRanking(null, null, null, null, null, null))
                .thenReturn(emptyPage);

        ResponseEntity<?> response = controller.getProfessionalRanking(null, null, null, null, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("getRankingTournaments returns mapped tournament summaries")
    void shouldReturnTournamentSummaries() {
        TournamentSummary summary = new TournamentSummary(
                UUID.randomUUID(), "Tournament A",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 25),
                Surface.CLAY, 32, "Madrid", TournamentStatus.OPEN, true);

        when(tournamentService.findSummaries()).thenReturn(List.of(summary));

        ResponseEntity<?> response = controller.getRankingTournaments();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getRankingTournaments returns empty list when no tournaments")
    void shouldReturnEmptyTournamentSummaries() {
        when(tournamentService.findSummaries()).thenReturn(List.of());

        ResponseEntity<?> response = controller.getRankingTournaments();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("getTournamentRanking returns mapped page response")
    void shouldReturnTournamentRankingPage() {
        UUID tournamentId = UUID.randomUUID();
        TournamentRankingEntry entry = new TournamentRankingEntry(
                1, UUID.randomUUID(), "LIC-002", "Jane", "Smith", "FEMALE", 5L);
        RankingPage<TournamentRankingEntry> page = new RankingPage<>(
                List.of(entry), 0, 10, 1, 1, "victories", "desc");

        when(rankingService.findTournamentRanking(tournamentId, "FEMALE", 2, 0, 10, "victories", "desc"))
                .thenReturn(page);

        ResponseEntity<?> response = controller.getTournamentRanking(
                tournamentId, "FEMALE", 2, 0, 10, "victories", "desc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getTournamentRanking with null params uses defaults")
    void shouldReturnTournamentRankingWithNullParams() {
        UUID tournamentId = UUID.randomUUID();
        RankingPage<TournamentRankingEntry> emptyPage = new RankingPage<>(
                List.of(), 0, 10, 0, 0, "victories", "desc");

        when(rankingService.findTournamentRanking(tournamentId, null, null, null, null, null, null))
                .thenReturn(emptyPage);

        ResponseEntity<?> response = controller.getTournamentRanking(
                tournamentId, null, null, null, null, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("getTournamentRanking maps entry fields correctly")
    void shouldMapTournamentRankingFields() {
        UUID tournamentId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        TournamentRankingEntry entry = new TournamentRankingEntry(
                3, participantId, "LIC-003", "Carlos", "Ruiz", "MALE", 2L);
        RankingPage<TournamentRankingEntry> page = new RankingPage<>(
                List.of(entry), 0, 10, 1, 1, "victories", "desc");

        when(rankingService.findTournamentRanking(tournamentId, null, null, null, null, null, null))
                .thenReturn(page);

        ResponseEntity<?> response = controller.getTournamentRanking(
                tournamentId, null, null, null, null, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("getProfessionalRanking maps all entry fields")
    void shouldMapAllProfessionalRankingFields() {
        ProfessionalRankingEntry entry = new ProfessionalRankingEntry(
                5, 200, "LIC-005", "Maria Garcia", "Maria", "Garcia", "FEMALE", "B",
                "Club Tenis", LocalDate.of(1995, 6, 15), 1200);
        RankingPage<ProfessionalRankingEntry> page = new RankingPage<>(
                List.of(entry), 0, 10, 1, 1, "position", "asc");

        when(rankingService.findProfessionalRanking("FEMALE", "B", 0, 10, "position", "asc"))
                .thenReturn(page);

        ResponseEntity<?> response = controller.getProfessionalRanking("FEMALE", "B", 0, 10, "position", "asc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("getTournamentRanking with multiple entries maps all")
    void shouldMapMultipleTournamentEntries() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                new TournamentRankingEntry(1, UUID.randomUUID(), "L1", "A", "B", "MALE", 5L),
                new TournamentRankingEntry(2, UUID.randomUUID(), "L2", "C", "D", "FEMALE", 3L),
                new TournamentRankingEntry(3, UUID.randomUUID(), "L3", "E", "F", "MALE", 1L)
        );
        RankingPage<TournamentRankingEntry> page = new RankingPage<>(
                entries, 0, 10, 3, 1, "victories", "desc");

        when(rankingService.findTournamentRanking(tournamentId, null, null, null, null, null, null))
                .thenReturn(page);

        ResponseEntity<?> response = controller.getTournamentRanking(
                tournamentId, null, null, null, null, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("getRankingTournaments maps all summary fields")
    void shouldMapAllTournamentSummaryFields() {
        TournamentSummary summary = new TournamentSummary(
                UUID.randomUUID(), "Grand Slam",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14),
                LocalTime.of(10, 30),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 28),
                Surface.HARD, 128, "Barcelona", TournamentStatus.CLOSED, false);

        when(tournamentService.findSummaries()).thenReturn(List.of(summary));

        ResponseEntity<?> response = controller.getRankingTournaments();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
