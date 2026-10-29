package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.ranking.ProfessionalRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.port.out.RankingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RankingRepository rankingRepository;

    @InjectMocks
    private RankingService rankingService;

    @Test
    void findTournamentRankingShouldAssignSamePositionForEqualVictories() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> repositoryEntries = List.of(
                entry(3L, 50L),
                entry(3L, 50L),
                entry(1L, 20L)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(repositoryEntries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, " MALE ", 7, 0, 10, null, null);

        assertEquals(10, result.size());
        assertEquals(3, result.totalItems());
        assertEquals(1, result.items().get(0).position());
        assertEquals(1, result.items().get(1).position());
        assertEquals(3, result.items().get(2).position());
    }

    @Test
    void findProfessionalRankingShouldReturnPaginatedResults() {
        RankingPage<ProfessionalRankingEntry> page = new RankingPage<>(
                List.of(new ProfessionalRankingEntry(1, 100, "P1", "Player", "P", "L", "MALE", "Absoluta", "Club", null, 100)),
                0, 10, 2, 1, "points", "asc"
        );
        when(rankingRepository.findProfessionalRanking("MALE", null, 0, 10, "points", "asc")).thenReturn(page);

        RankingPage<ProfessionalRankingEntry> result = rankingService.findProfessionalRanking("MALE", null, 0, 10, "points", "asc");

        assertEquals(2, result.totalItems());
    }

    @Test
    void findProfessionalRankingWithNullGender() {
        when(rankingRepository.findProfessionalRanking(null, null, 0, 10, "position", "asc"))
                .thenReturn(new RankingPage<>(List.of(), 0, 10, 0, 0, "position", "asc"));

        RankingPage<ProfessionalRankingEntry> result = rankingService.findProfessionalRanking(null, null, null, null, null, null);

        assertEquals(0, result.totalItems());
    }

    @Test
    void findTournamentRankingWithSortByName() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "Alice", null, "MALE", null, 5L),
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "Bob", null, "MALE", null, 3L)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(entries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, "name", "ASC");

        assertEquals(2, result.totalItems());
    }

    @Test
    void findTournamentRankingWithSortByGender() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "Alice", null, "FEMALE", null, 5L),
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "Bob", null, "MALE", null, 3L)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(entries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, "gender", "DESC");

        assertEquals(2, result.totalItems());
    }

    @Test
    void findTournamentRankingWithSortByPosition() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                new TournamentRankingEntry(1, UUID.randomUUID(), null, "Alice", null, "MALE", null, 5L),
                new TournamentRankingEntry(2, UUID.randomUUID(), null, "Bob", null, "MALE", null, 3L)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(entries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, "position", "ASC");

        assertEquals(2, result.totalItems());
    }

    @Test
    void findTournamentRankingWithNullPagination() {
        UUID tournamentId = UUID.randomUUID();
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(List.of());

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, null, null, null, null);

        assertEquals(0, result.totalItems());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void findTournamentRankingWithEmptyResults() {
        UUID tournamentId = UUID.randomUUID();
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(List.of());

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, null, null);

        assertEquals(0, result.totalItems());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void findProfessionalRankingWithSortByName() {
        RankingPage<ProfessionalRankingEntry> page = new RankingPage<>(
                List.of(new ProfessionalRankingEntry(1, 100, "P1", "Alice", "A", "L", "MALE", "Absoluta", "Club", null, 100)),
                0, 10, 1, 1, "name", "asc"
        );
        when(rankingRepository.findProfessionalRanking("MALE", null, 0, 10, "name", "asc")).thenReturn(page);

        RankingPage<ProfessionalRankingEntry> result = rankingService.findProfessionalRanking("MALE", null, 0, 10, "name", "ASC");

        assertEquals(1, result.totalItems());
    }

    @Test
    void findProfessionalRankingWithSortByRankingPosition() {
        RankingPage<ProfessionalRankingEntry> page = new RankingPage<>(
                List.of(new ProfessionalRankingEntry(1, 100, "P1", "Alice", "A", "L", "MALE", "Absoluta", "Club", null, 100)),
                0, 10, 1, 1, "position", "desc"
        );
        when(rankingRepository.findProfessionalRanking("MALE", null, 0, 10, "position", "desc")).thenReturn(page);

        RankingPage<ProfessionalRankingEntry> result = rankingService.findProfessionalRanking("MALE", null, 0, 10, "position", "DESC");

        assertEquals(1, result.totalItems());
    }

    @Test
    void findProfessionalRankingWithFilterByCategory() {
        RankingPage<ProfessionalRankingEntry> page = new RankingPage<>(
                List.of(new ProfessionalRankingEntry(1, 100, "P1", "Alice", "A", "L", "FEMALE", "Absoluta", "Club", null, 100)),
                0, 10, 1, 1, "position", "asc"
        );
        when(rankingRepository.findProfessionalRanking(null, "ABSOLUTA", 0, 10, "position", "asc")).thenReturn(page);

        RankingPage<ProfessionalRankingEntry> result = rankingService.findProfessionalRanking(null, "Absoluta", 0, 10, null, null);

        assertEquals(1, result.totalItems());
    }

    @Test
    void findTournamentRankingSortsByPointsDescending() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "Low", null, "MALE", 10L, null),
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "High", null, "MALE", 50L, null),
                new TournamentRankingEntry(null, UUID.randomUUID(), null, "Mid", null, "MALE", 30L, null)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(entries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, null, null);

        assertEquals(50L, result.items().get(0).points());
        assertEquals(30L, result.items().get(1).points());
        assertEquals(10L, result.items().get(2).points());
    }

    @Test
    void findTournamentRankingAssignsSamePositionForEqualPoints() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                entry(50L),
                entry(50L),
                entry(20L)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(entries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, null, null);

        assertEquals(1, result.items().get(0).position());
        assertEquals(1, result.items().get(1).position());
        assertEquals(3, result.items().get(2).position());
    }

    @Test
    void findTournamentRankingWithPointsAndSortByPosition() {
        UUID tournamentId = UUID.randomUUID();
        List<TournamentRankingEntry> entries = List.of(
                new TournamentRankingEntry(1, UUID.randomUUID(), null, "Alice", null, "MALE", 100L, null),
                new TournamentRankingEntry(2, UUID.randomUUID(), null, "Bob", null, "MALE", 80L, null)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(entries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, "MALE", 7, 0, 10, "position", "ASC");

        assertEquals(100L, result.items().get(0).points());
        assertEquals(80L, result.items().get(1).points());
    }

    private TournamentRankingEntry entry(Long points) {
        return new TournamentRankingEntry(null, UUID.randomUUID(), null, "Player", null, "MALE", points, null);
    }

    private TournamentRankingEntry entry(Long victories, Long points) {
        return new TournamentRankingEntry(null, UUID.randomUUID(), null, "Player", null, "MALE", points, victories);
    }
}
