package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.club.ClubRankingEntry;
import com.tfm.tennis_platform.domain.port.out.ClubRankingRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClubRankingRepositoryAdapter implements ClubRankingRepository {

    private final JpaMatchRepository matchRepository;

    @Override
    public List<ClubRankingEntry> findClubRanking(UUID clubId, String gender) {
        return matchRepository.findClubRanking(clubId, gender).stream()
                .map(row -> new ClubRankingEntry(
                        null,
                        row.getPersonId(),
                        row.getFirstName(),
                        row.getLastName(),
                        row.getGender(),
                        row.getLicense(),
                        row.getPoints(),
                        row.getVictories(),
                        row.getTournaments()
                ))
                .toList();
    }
}
