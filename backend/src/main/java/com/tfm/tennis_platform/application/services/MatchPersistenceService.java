package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchPersistenceService {

    private final MatchRepository matchRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MatchPersistenceService.class);

    @Transactional
    public List<Match> saveMatches(List<Match> matches) {
        if (matches == null || matches.isEmpty()) return List.of();

        int attempts = 0;
        final int maxAttempts = 3;
        while (true) {
            try {
                log.debug("Persisting {} matches in REQUIRES_NEW transaction", matches.size());
                return matchRepository.saveAll(matches);
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempts++;
                log.warn("Optimistic locking conflict persisting matches (attempt {}/{}): {}", attempts, maxAttempts, ex.getMessage());
                if (attempts >= maxAttempts) {
                    log.error("Max attempts reached when persisting matches");
                    throw ex;
                }
                try { Thread.sleep(100L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }
}
