package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.ranking.ProfessionalRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;
import com.tfm.tennis_platform.domain.port.out.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final RankingRepository rankingRepository;

    @Transactional(readOnly = true)
    public RankingPage<ProfessionalRankingEntry> findProfessionalRanking(
            String gender,
            String category,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        String normalizedSortBy = normalizeProfessionalSort(sortBy);
        String normalizedSortDirection = normalizeSortDirection(sortDirection, "asc");
        return rankingRepository.findProfessionalRanking(
                normalizeFilter(gender),
                normalizeFilter(category),
                normalizePage(page),
                normalizeSize(size),
                normalizedSortBy,
                normalizedSortDirection
        );
    }

    @Transactional(readOnly = true)
    public RankingPage<TournamentRankingEntry> findTournamentRanking(
            UUID tournamentId,
            String gender,
            Integer categoryId,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        String normalizedSortBy = normalizeTournamentSort(sortBy);
        String normalizedSortDirection = normalizeSortDirection(sortDirection, "desc");
        List<TournamentRankingEntry> entries = rankingRepository.findTournamentRanking(tournamentId, normalizeFilter(gender), categoryId);
        List<TournamentRankingEntry> ranked = sortTournamentEntries(
                assignTournamentPositions(entries),
                normalizedSortBy,
                normalizedSortDirection
        );
        long totalItems = ranked.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / normalizedSize);
        int fromIndex = Math.min(normalizedPage * normalizedSize, ranked.size());
        int toIndex = Math.min(fromIndex + normalizedSize, ranked.size());

        return new RankingPage<>(
                ranked.subList(fromIndex, toIndex),
                normalizedPage,
                normalizedSize,
                totalItems,
                totalPages,
                normalizedSortBy,
                normalizedSortDirection
        );
    }

    private List<TournamentRankingEntry> assignTournamentPositions(List<TournamentRankingEntry> entries) {
        List<TournamentRankingEntry> ranked = new ArrayList<>();
        Long previousPoints = null;
        int currentPosition = 0;

        List<TournamentRankingEntry> rankingOrder = entries.stream()
                .sorted(tournamentRankingComparator("points", "desc"))
                .toList();

        for (int index = 0; index < rankingOrder.size(); index++) {
            TournamentRankingEntry entry = rankingOrder.get(index);
            long entryPoints = entry.points() != null ? entry.points() : 0L;
            if (previousPoints == null || !previousPoints.equals(entryPoints)) {
                currentPosition = index + 1;
                previousPoints = entryPoints;
            }
            ranked.add(entry.withPosition(currentPosition));
        }

        return ranked;
    }

    private List<TournamentRankingEntry> sortTournamentEntries(
            List<TournamentRankingEntry> entries,
            String sortBy,
            String sortDirection
    ) {
        return entries.stream()
                .sorted(tournamentRankingComparator(sortBy, sortDirection))
                .toList();
    }

    private Comparator<TournamentRankingEntry> tournamentRankingComparator(String sortBy, String sortDirection) {
        boolean descending = "desc".equals(sortDirection);
        Comparator<TournamentRankingEntry> nameComparator = Comparator
                .comparing(TournamentRankingEntry::lastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(TournamentRankingEntry::firstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

        return switch (sortBy) {
            case "name" -> descending ? nameComparator.reversed() : nameComparator;
            case "gender" -> directionAware(
                    Comparator.comparing(TournamentRankingEntry::gender, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)),
                    descending
            ).thenComparingInt(entry -> entry.position() == null ? Integer.MAX_VALUE : entry.position());
            case "position" -> directionAware(
                    Comparator.comparingInt((TournamentRankingEntry entry) -> entry.position() == null ? Integer.MAX_VALUE : entry.position()),
                    descending
            ).thenComparing(nameComparator);
            case "victories" -> directionAware(
                    Comparator.comparingLong((TournamentRankingEntry entry) -> entry.victories() == null ? 0L : entry.victories()),
                    descending
            ).thenComparing(nameComparator);
            default -> directionAware(
                    Comparator.comparingLong((TournamentRankingEntry entry) -> entry.points() == null ? 0L : entry.points()),
                    descending
            ).thenComparing(nameComparator);
        };
    }

    private Comparator<TournamentRankingEntry> directionAware(
            Comparator<TournamentRankingEntry> comparator,
            boolean descending
    ) {
        return descending ? comparator.reversed() : comparator;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private String normalizeProfessionalSort(String sortBy) {
        return switch (normalizeSortValue(sortBy)) {
            case "name", "points", "category", "gender" -> normalizeSortValue(sortBy);
            default -> "position";
        };
    }

    private String normalizeTournamentSort(String sortBy) {
        return switch (normalizeSortValue(sortBy)) {
            case "position", "name", "gender", "victories" -> normalizeSortValue(sortBy);
            default -> "points";
        };
    }

    private String normalizeSortDirection(String sortDirection, String defaultDirection) {
        return "asc".equalsIgnoreCase(sortDirection) || "desc".equalsIgnoreCase(sortDirection)
                ? sortDirection.trim().toLowerCase(Locale.ROOT)
                : defaultDirection;
    }

    private String normalizeSortValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}
