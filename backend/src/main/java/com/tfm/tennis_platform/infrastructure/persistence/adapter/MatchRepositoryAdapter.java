package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MatchDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MatchRepositoryAdapter implements MatchRepository {

    private static final Logger log = LoggerFactory.getLogger(MatchRepositoryAdapter.class);

    private final JpaMatchRepository matchRepository;
    private final MatchDomainMapper mapper;

    @Override
    public Match save(Match match) {
        return mapper.toDomain(matchRepository.save(mapper.toEntity(match)));
    }

    @Override
    public List<Match> saveAll(List<Match> matches) {
        if (matches == null || matches.isEmpty()) return List.of();
        List<com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity> entities = matches.stream()
            .map(mapper::toEntity)
            .toList();

        // Log ids for diagnostics
        log.debug("Saving {} matches: {}", entities.size(), entities.stream().map(e -> e.getId()).toList());

        int attempts = 0;
        final int maxAttempts = 3;
        while (true) {
            try {
                List<com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity> saved = matchRepository.saveAll(entities);
                log.debug("Saved {} matches successfully", saved.size());
                return saved.stream().map(mapper::toDomain).toList();
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic locking while saving matches: attempt {} error={}", attempts + 1, ex.getMessage());
                attempts++;
                if (attempts >= maxAttempts) {
                    throw ex;
                }
                try { Thread.sleep(100L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    @Override
    public List<Match> findByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Match> findById(String id) {
        try {
            return matchRepository.findById(UUID.fromString(id)).map(mapper::toDomain);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
