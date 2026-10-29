package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.List;

public record RankingPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        String sortBy,
        String sortDirection
) {}
