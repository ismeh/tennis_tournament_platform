package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;

    public Match update(Match match) {
        return matchRepository.save(match);
    }

    @Transactional
    public Match recordResult(UUID tournamentId, UUID matchId, UUID winnerId, String scoreString) {
        if (tournamentId == null) {
            throw new InvalidArgumentException("Tournament id must not be null");
        }
        if (matchId == null) {
            throw new InvalidArgumentException("Match id must not be null");
        }
        if (winnerId == null) {
            throw new InvalidArgumentException("Winner id must not be null");
        }
        if (scoreString == null || scoreString.isBlank()) {
            throw new InvalidArgumentException("Score must not be blank");
        }

        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        Match currentMatch = matchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> matchId.equals(match.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));

        if (!winnerId.equals(currentMatch.getFirstInscriptionId()) && !winnerId.equals(currentMatch.getSecondInscriptionId())) {
            throw new InvalidArgumentException("Winner must be one of the participants in the match");
        }

        Match updatedCurrentMatch = currentMatch.toBuilder()
                .winner(createInscriptionReference(winnerId))
                .result(scoreString.trim())
                .build();

        Match savedCurrentMatch = matchRepository.save(updatedCurrentMatch);

        if (currentMatch.getNextMatch() != null && currentMatch.getNextMatch().getId() != null) {
            Match nextMatch = matchRepository.findById(currentMatch.getNextMatch().getId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Next match", currentMatch.getNextMatch().getId()));

            Match updatedNextMatch = placeWinnerInNextMatch(nextMatch, winnerId);
            matchRepository.save(updatedNextMatch);
        }

        return savedCurrentMatch;
    }

    public List<Match> findByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId);
    }

    public Optional<Match> findById(String id) {
        return matchRepository.findById(id);
    }

    private Match placeWinnerInNextMatch(Match nextMatch, UUID winnerId) {
        if (winnerId.equals(nextMatch.getFirstInscriptionId()) || winnerId.equals(nextMatch.getSecondInscriptionId())) {
            return nextMatch;
        }

        if (nextMatch.getFirstInscriptionId() == null) {
            return nextMatch.toBuilder()
                    .firstInscription(createInscriptionReference(winnerId))
                    .build();
        }

        if (nextMatch.getSecondInscriptionId() == null) {
            return nextMatch.toBuilder()
                    .secondInscription(createInscriptionReference(winnerId))
                    .build();
        }

        throw new InvalidArgumentException("The next match already has two participants assigned");
    }

    private Inscription createInscriptionReference(UUID inscriptionId) {
        return Inscription.builder()
                .id(inscriptionId)
                .build();
    }
}
