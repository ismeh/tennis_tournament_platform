package com.tfm.tennis_platform.application.services;

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
                entry(3L),
                entry(3L),
                entry(1L)
        );
        when(rankingRepository.findTournamentRanking(tournamentId, "MALE", 7)).thenReturn(repositoryEntries);

        RankingPage<TournamentRankingEntry> result = rankingService.findTournamentRanking(tournamentId, " MALE ", 7, 0, 10, null, null);

        assertEquals(10, result.size());
        assertEquals(3, result.totalItems());
        assertEquals(1, result.items().get(0).position());
        assertEquals(1, result.items().get(1).position());
        assertEquals(3, result.items().get(2).position());
    }

    private TournamentRankingEntry entry(Long victories) {
        return new TournamentRankingEntry(null, UUID.randomUUID(), null, "Player", null, "MALE", victories);
    }
}
