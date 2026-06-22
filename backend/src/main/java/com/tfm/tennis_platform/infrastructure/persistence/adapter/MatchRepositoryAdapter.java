package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MatchDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
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
        if (match.getLoserNextMatch() != null && match.getLoserNextMatch().getId() != null) {
            entity.setLoserNextMatch(matchRepository.getReferenceById(match.getLoserNextMatch().getId()));
        }
        return mapper.toDomain(matchRepository.save(entity));
    }

    @Override
    @Transactional
    public List<Match> saveAll(List<Match> matches) {
        if (matches == null || matches.isEmpty()) return List.of();

        java.util.Map<UUID, com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity> entityById =
                new java.util.LinkedHashMap<>();

        for (Match domain : matches) {
            com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity entity = null;
            if (domain.getId() != null) {
                entity = matchRepository.findById(domain.getId()).orElse(null);
            }
            if (entity == null) {
                entity = mapper.toEntity(domain);
            } else {
                copyState(entity, mapper.toEntity(domain));
            }
            entityById.put(entity.getId(), entity);
        }

        for (Match domain : matches) {
            com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity entity = entityById.get(domain.getId());
            if (domain.getNextMatch() != null && domain.getNextMatch().getId() != null) {
                com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity next = entityById.get(domain.getNextMatch().getId());
                if (next != null) {
                    entity.setNextMatch(next);
                } else {
                    entity.setNextMatch(matchRepository.getReferenceById(domain.getNextMatch().getId()));
                }
            }
            if (domain.getLoserNextMatch() != null && domain.getLoserNextMatch().getId() != null) {
                com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity next = entityById.get(domain.getLoserNextMatch().getId());
                if (next != null) {
                    entity.setLoserNextMatch(next);
                } else {
                    entity.setLoserNextMatch(matchRepository.getReferenceById(domain.getLoserNextMatch().getId()));
                }
            }
        }

        List<com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity> entities =
                new java.util.ArrayList<>(entityById.values());

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
        target.setBracketPosition(source.getBracketPosition());
        target.setScheduledAt(source.getScheduledAt());
        target.setScheduleTimeType(source.getScheduleTimeType());
        target.setCourtResource(source.getCourtResource());
        target.setCourt(source.getCourt());
        target.setResult(source.getResult());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findById(String id) {
        try {
            return matchRepository.findById(UUID.fromString(id)).map(mapper::toDomain);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findByIdAndTournamentId(UUID matchId, UUID tournamentId) {
        if (matchId == null || tournamentId == null) {
            return Optional.empty();
        }

        return matchRepository.findByIdAndTournamentId(matchId, tournamentId).map(mapper::toDomain);
    }
}
