package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.domain.port.out.ProPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProPlayerQueryService {

    private final ProPlayerRepository proPlayerRepository;

    @Transactional(readOnly = true)
    public List<ProPlayer> search(String query) {
        return search(query, null, null);
    }

    @Transactional(readOnly = true)
    public List<ProPlayer> search(String query, String gender, String category) {
        String normalizedQuery = normalize(query);
        String normalizedGender = normalizeUpper(gender);
        String normalizedCategory = normalizeUpper(category);

        if (normalizedQuery == null && normalizedGender == null && normalizedCategory == null) {
            return proPlayerRepository.findTop10();
        }

        return proPlayerRepository.search(normalizedQuery, normalizedGender, normalizedCategory);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
