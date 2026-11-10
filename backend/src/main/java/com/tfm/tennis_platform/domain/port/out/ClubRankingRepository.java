package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.club.ClubRankingEntry;

import java.util.List;
import java.util.UUID;

public interface ClubRankingRepository {
    List<ClubRankingEntry> findClubRanking(UUID clubId, String gender);
}
