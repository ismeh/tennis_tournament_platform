package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SingleEliminationMatchGenerator implements MatchGenerationStrategy {

    @Override
    public List<Match> generateMatches(Draw draw, List<Inscription> inscriptions) {
        List<Match> allMatches = new ArrayList<>();
        
        if (inscriptions.isEmpty()) {
            return allMatches;
        }

        int participantCount = inscriptions.size();
        int rounds = calculateRounds(participantCount);

        // Store match IDs by position for linking
        Map<String, UUID> matchPositions = new LinkedHashMap<>();
        Map<String, Match> matchMap = new LinkedHashMap<>();

        // First pass: Generate all matches with IDs
        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = calculateMatchesInRound(participantCount, round);
            
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                String position = round + "-" + matchIndex;
                UUID matchId = UUID.randomUUID();
                matchPositions.put(position, matchId);
                
                Match match = Match.builder()
                    .id(matchId)
                    .drawId(draw.getId())
                    .roundNumber(round)
                    .build();
                matchMap.put(position, match);
            }
        }

        // Second pass: connect each match with its next-round destination.
        for (int round = 1; round < rounds; round++) {
            int matchesInRound = calculateMatchesInRound(participantCount, round);

            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                String currentPosition = round + "-" + matchIndex;
                String nextPosition = (round + 1) + "-" + (matchIndex / 2);

                Match currentMatch = matchMap.get(currentPosition);
                Match nextMatch = matchMap.get(nextPosition);

                if (currentMatch != null && nextMatch != null) {
                    matchMap.put(currentPosition, currentMatch.toBuilder()
                            .nextMatch(nextMatch)
                            .build());
                }
            }
        }

        // Third pass: seed inscriptions into first-round matches (firstInscription / secondInscription)
        int matchesInFirstRound = calculateMatchesInRound(participantCount, 1);
        for (int i = 0; i < inscriptions.size(); i++) {
            Inscription inscription = inscriptions.get(i);
            int matchIndex = i / 2;
            String position = 1 + "-" + matchIndex;
            Match existing = matchMap.get(position);
            if (existing == null) {
                continue;
            }

            Match updated;
            if (i % 2 == 0) {
                updated = existing.toBuilder().firstInscription(inscription).build();
            } else {
                updated = existing.toBuilder().secondInscription(inscription).build();
            }
            matchMap.put(position, updated);
        }

        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = calculateMatchesInRound(participantCount, round);
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                Match match = matchMap.get(round + "-" + matchIndex);
                if (match != null) {
                    allMatches.add(match);
                }
            }
        }
        return allMatches;
    }

    private int calculateRounds(int participantCount) {
        if (participantCount <= 0) {
            return 0;
        }
        if (participantCount == 1) {
            return 0;
        }
        return (int) Math.ceil(Math.log(participantCount) / Math.log(2));
    }

    private int calculateMatchesInRound(int participantCount, int round) {
        int rounds = calculateRounds(participantCount);
        if (round <= 0 || round > rounds) {
            return 0;
        }
        
        // First round: ceil(participantCount / 2)
        if (round == 1) {
            return (participantCount + 1) / 2;
        }
        
        // Subsequent rounds: half of previous round
        return calculateMatchesInRound(participantCount, round - 1) / 2;
    }
}
