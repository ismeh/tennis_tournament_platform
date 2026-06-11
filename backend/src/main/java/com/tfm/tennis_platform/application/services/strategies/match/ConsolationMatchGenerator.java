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
public class ConsolationMatchGenerator implements MatchGenerationStrategy {

    @Override
    public List<Match> generateMatches(Draw draw, List<Inscription> inscriptions) {
        int eligibleLosers = calculateFirstRoundLosers(inscriptions.size());
        if (eligibleLosers < 2) {
            return List.of();
        }

        int bracketSize = calculateBracketSize(eligibleLosers);
        int rounds = calculateRounds(bracketSize);
        Map<String, Match> matchMap = new LinkedHashMap<>();

        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = calculateMatchesInRound(bracketSize, round);
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                String position = round + "-" + matchIndex;
                matchMap.put(position, Match.builder()
                        .id(UUID.randomUUID())
                        .drawId(draw.getId())
                        .roundNumber(round)
                        .bracketPosition(matchIndex)
                        .build());
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

        List<Match> matches = new ArrayList<>();
        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = calculateMatchesInRound(bracketSize, round);
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                Match match = matchMap.get(round + "-" + matchIndex);
                if (match != null) {
                    matches.add(match);
                }
            }
        }

        return matches;
    }

    private int calculateFirstRoundLosers(int participantCount) {
        int bracketSize = calculateBracketSize(participantCount);
        int firstRoundMatches = calculateMatchesInRound(bracketSize, 1);
        int byeCount = bracketSize - participantCount;
        return Math.max(0, firstRoundMatches - byeCount);
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
