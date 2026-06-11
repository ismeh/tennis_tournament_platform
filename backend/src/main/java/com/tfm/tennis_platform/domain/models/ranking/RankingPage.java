package com.tfm.tennis_platform.domain.models.ranking;

import java.util.List;

public record RankingPage<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        String sortBy,
        String sortDirection
) {}
