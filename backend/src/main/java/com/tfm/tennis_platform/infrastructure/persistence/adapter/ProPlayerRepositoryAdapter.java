package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.domain.port.out.ProPlayerRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ProPlayerMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProPlayerRepositoryAdapter implements ProPlayerRepository {

    private static final int SEARCH_LIMIT = 10;

    private final JpaProPlayerRepository jpaProPlayerRepository;
    private final ProPlayerMapper proPlayerMapper;

    @Override
    public Optional<ProPlayer> findById(Integer id) {
        return jpaProPlayerRepository.findById(id).map(proPlayerMapper::toDomain);
    }

    @Override
    public Optional<ProPlayer> findByLicense(String license) {
        if (license == null || license.isBlank()) {
            return Optional.empty();
        }

        return jpaProPlayerRepository.findFirstByLicenseIgnoreCase(license.trim())
                .map(proPlayerMapper::toDomain);
    }

    @Override
    public List<ProPlayer> findTop10() {
        return jpaProPlayerRepository.findTop10ByOrderByRankingPositionAsc().stream()
                .map(proPlayerMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProPlayer> searchByQuery(String query) {
        return jpaProPlayerRepository.searchByQuery(query, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(proPlayerMapper::toDomain)
                .toList();
    }
}
