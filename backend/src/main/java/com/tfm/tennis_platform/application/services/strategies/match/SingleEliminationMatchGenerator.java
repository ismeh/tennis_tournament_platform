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
        int bracketSize = calculateBracketSize(participantCount);
        int rounds = calculateRounds(bracketSize);

        Map<String, Match> matchMap = new LinkedHashMap<>();

        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = calculateMatchesInRound(bracketSize, round);
            
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                String position = round + "-" + matchIndex;
                UUID matchId = UUID.randomUUID();

                Match match = Match.builder()
                    .id(matchId)
                    .drawId(draw.getId())
                    .roundNumber(round)
                    .build();
                matchMap.put(position, match);
            }
        }

        for (int round = 1; round < rounds; round++) {
            int matchesInRound = calculateMatchesInRound(bracketSize, round);

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

        int matchesInFirstRound = calculateMatchesInRound(bracketSize, 1);
        int byeCount = bracketSize - participantCount;
        int inscriptionIndex = 0;
        for (int matchIndex = 0; matchIndex < matchesInFirstRound; matchIndex++) {
            String position = 1 + "-" + matchIndex;
            Match existing = matchMap.get(position);
            if (existing == null) {
                continue;
            }

            if (matchIndex < byeCount) {
                Inscription inscription = inscriptions.get(inscriptionIndex++);
                matchMap.put(position, existing.toBuilder()
                        .firstInscription(inscription)
                        .winner(inscription)
                        .build());
            } else {
                Inscription firstInscription = inscriptions.get(inscriptionIndex++);
                Inscription secondInscription = inscriptionIndex < inscriptions.size()
                        ? inscriptions.get(inscriptionIndex++)
                        : null;
                matchMap.put(position, existing.toBuilder()
                        .firstInscription(firstInscription)
                        .secondInscription(secondInscription)
                        .build());
            }
        }

        for (int matchIndex = 0; matchIndex < matchesInFirstRound; matchIndex++) {
            Match match = matchMap.get(1 + "-" + matchIndex);
            if (match == null) {
                continue;
            }

            Inscription advancingInscription = getByeAdvancingInscription(match);
            if (advancingInscription != null) {
                advanceInscription(matchMap, 1, matchIndex, advancingInscription);
            }
        }

        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = calculateMatchesInRound(bracketSize, round);
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                Match match = matchMap.get(round + "-" + matchIndex);
                if (match != null) {
                    allMatches.add(match);
                }
            }
        }
        return allMatches;
    }

    private void advanceInscription(Map<String, Match> matchMap, int round, int matchIndex, Inscription inscription) {
        String nextPosition = (round + 1) + "-" + (matchIndex / 2);
        Match nextMatch = matchMap.get(nextPosition);
        if (nextMatch == null) {
            return;
        }

        Match updatedNextMatch = matchIndex % 2 == 0
                ? nextMatch.toBuilder().firstInscription(inscription).build()
                : nextMatch.toBuilder().secondInscription(inscription).build();
        matchMap.put(nextPosition, updatedNextMatch);
    }

    private Inscription getByeAdvancingInscription(Match match) {
        if (match.getFirstInscription() != null && match.getSecondInscription() == null) {
            return match.getFirstInscription();
        }

        if (match.getFirstInscription() == null && match.getSecondInscription() != null) {
            return match.getSecondInscription();
        }

        return null;
    }

    private int calculateBracketSize(int participantCount) {
        int bracketSize = 1;
        while (bracketSize < participantCount) {
            bracketSize *= 2;
        }
        return bracketSize;
    }

    private int calculateRounds(int bracketSize) {
        if (bracketSize <= 1) {
            return 0;
        }
        return (int) (Math.log(bracketSize) / Math.log(2));
    }

    private int calculateMatchesInRound(int bracketSize, int round) {
        int rounds = calculateRounds(bracketSize);
        if (round <= 0 || round > rounds) {
            return 0;
        }

        return bracketSize / (int) Math.pow(2, round);
    }
}
