package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.ranking.ProfessionalRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;

import java.util.List;
import java.util.UUID;

public interface RankingRepository {
    RankingPage<ProfessionalRankingEntry> findProfessionalRanking(
            String gender,
            String category,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );
    List<TournamentRankingEntry> findTournamentRanking(UUID tournamentId, String gender, Integer categoryId);
}
