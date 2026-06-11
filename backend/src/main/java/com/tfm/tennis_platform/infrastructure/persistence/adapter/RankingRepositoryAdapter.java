package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.domain.models.ranking.ProfessionalRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;
import com.tfm.tennis_platform.domain.port.out.RankingRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ProPlayerMapper;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.TournamentRankingProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RankingRepositoryAdapter implements RankingRepository {

    private final JpaProPlayerRepository proPlayerRepository;
    private final JpaMatchRepository matchRepository;
    private final ProPlayerMapper proPlayerMapper;

    @Override
    public RankingPage<ProfessionalRankingEntry> findProfessionalRanking(
            String gender,
            String category,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Page<ProPlayerEntity> players = proPlayerRepository.findRanking(
                        gender,
                        category,
                        PageRequest.of(page, size, toProfessionalSort(sortBy, sortDirection))
                );
        List<ProfessionalRankingEntry> items = players.stream()
                .map(proPlayerMapper::toDomain)
                .map(this::toProfessionalRankingEntry)
                .toList();

        return new RankingPage<>(
                items,
                players.getNumber(),
                players.getSize(),
                players.getTotalElements(),
                players.getTotalPages(),
                sortBy,
                sortDirection
        );
    }

    @Override
    public List<TournamentRankingEntry> findTournamentRanking(UUID tournamentId, String gender, Integer categoryId) {
        return matchRepository.findTournamentRanking(tournamentId, gender, categoryId).stream()
                .map(this::toTournamentRankingEntry)
                .toList();
    }

    private ProfessionalRankingEntry toProfessionalRankingEntry(ProPlayer player) {
        return new ProfessionalRankingEntry(
                player.getRankingPosition(),
                player.getId(),
                player.getLicense(),
                player.getFullName(),
                player.getFirstName(),
                player.getLastName(),
                player.getGender(),
                player.getAgeCategory(),
                player.getClubName(),
                player.getBirthDate(),
                player.getPoints()
        );
    }

    private TournamentRankingEntry toTournamentRankingEntry(TournamentRankingProjection projection) {
        return new TournamentRankingEntry(
                null,
                projection.getParticipantId(),
                projection.getLicense(),
                projection.getFirstName(),
                projection.getLastName(),
                projection.getGender(),
                projection.getVictories()
        );
    }

    private Sort toProfessionalSort(String sortBy, String sortDirection) {
        String property = switch (normalize(sortBy)) {
            case "name" -> "name";
            case "points" -> "points";
            case "category" -> "ageCategory";
            case "gender" -> "gender";
            default -> "rankingPosition";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort.Order primary = Sort.Order.by(property).with(direction).nullsLast();

        if ("name".equals(property)) {
            return Sort.by(primary);
        }

        return Sort.by(primary, Sort.Order.asc("name"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
