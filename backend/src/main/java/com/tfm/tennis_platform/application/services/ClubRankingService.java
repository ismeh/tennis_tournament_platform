package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.models.club.ClubRankingEntry;
import com.tfm.tennis_platform.domain.port.out.ClubRankingRepository;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubRankingService {

    private final ClubRepository clubRepository;
    private final ClubRankingRepository clubRankingRepository;

    @Transactional(readOnly = true)
    public List<ClubRankingEntry> findRanking(String slug, String gender) {
        Club club = clubRepository.findBySlug(slug)
                .filter(Club::isPublicPageEnabled)
                .orElseThrow(() -> new com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException(
                        "No se encontró la página pública del club solicitado."));

        return findRankingByClubId(club.getId(), gender);
    }

    @Transactional(readOnly = true)
    public List<ClubRankingEntry> findRankingByClubId(UUID clubId, String gender) {
        String normalizedGender = normalizeGender(gender);
        List<ClubRankingEntry> entries = clubRankingRepository.findClubRanking(clubId, normalizedGender);
        return assignPositions(entries);
    }

    private List<ClubRankingEntry> assignPositions(List<ClubRankingEntry> entries) {
        List<ClubRankingEntry> ranked = new ArrayList<>();
        Long previousPoints = null;
        int currentPosition = 0;

        for (int index = 0; index < entries.size(); index++) {
            ClubRankingEntry entry = entries.get(index);
            long points = entry.points() != null ? entry.points() : 0L;
            if (previousPoints == null || !previousPoints.equals(points)) {
                currentPosition = index + 1;
                previousPoints = points;
            }
            ranked.add(entry.withPosition(currentPosition));
        }
        return ranked;
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        String normalized = gender.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("MALE") || normalized.equals("FEMALE") || normalized.equals("MIXED")
                ? normalized
                : null;
    }
}
