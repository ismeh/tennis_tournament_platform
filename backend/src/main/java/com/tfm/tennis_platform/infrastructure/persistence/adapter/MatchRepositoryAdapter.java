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
        if (match == null) {
            return null;
        }

        com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity entity = match.getId() != null
                ? matchRepository.findById(match.getId()).orElse(null)
                : null;

        if (entity == null) {
            return mapper.toDomain(matchRepository.save(mapper.toEntity(match)));
        }

        copyState(entity, mapper.toEntity(match));
        if (match.getNextMatch() != null && match.getNextMatch().getId() != null) {
            entity.setNextMatch(matchRepository.getReferenceById(match.getNextMatch().getId()));
        }
        return mapper.toDomain(matchRepository.save(entity));
    }

    @Override
    public List<Match> saveAll(List<Match> matches) {
        if (matches == null || matches.isEmpty()) return List.of();
        // Two-pass mapping: first create entities without nextMatch, then wire nextMatch to reuse same instances
        List<com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity> entities = matches.stream()
            .map(mapper::toEntityWithoutNextMatch)
            .toList();

        // build id -> entity map for wiring nextMatch
        java.util.Map<java.util.UUID, com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity> entityById =
                entities.stream().collect(java.util.stream.Collectors.toMap(e -> e.getId(), e -> e));

        // wire nextMatch references using the same entity instances
        for (int i = 0; i < matches.size(); i++) {
            Match domain = matches.get(i);
            com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity entity = entities.get(i);
            if (domain.getNextMatch() != null && domain.getNextMatch().getId() != null) {
                com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity next = entityById.get(domain.getNextMatch().getId());
                if (next != null) {
                    entity.setNextMatch(next);
                } else {
                    // fallback to a reference if next match not in this batch
                    entity.setNextMatch(matchRepository.getReferenceById(domain.getNextMatch().getId()));
                }
            }
        }

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

    private void copyState(com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity target,
                           com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity source) {
        target.setDraw(source.getDraw());
        target.setFirstInscription(source.getFirstInscription());
        target.setSecondInscription(source.getSecondInscription());
        target.setWinner(source.getWinner());
        target.setRoundNumber(source.getRoundNumber());
        target.setScheduledAt(source.getScheduledAt());
        target.setCourt(source.getCourt());
        target.setResult(source.getResult());
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
