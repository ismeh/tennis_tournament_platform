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
public class DoubleEliminationMatchGenerator implements MatchGenerationStrategy {

    @Override
    public List<Match> generateMatches(Draw draw, List<Inscription> inscriptions) {
        int playerCount = inscriptions.size();
        if (playerCount < 2) {
            return List.of();
        }

        int bracketSize = calculateBracketSize(playerCount);
        int winnersRounds = calculateRounds(bracketSize);
        int losersRounds = 2 * (winnersRounds - 1);

        if (losersRounds <= 0) {
            return List.of();
        }

        Map<String, Match> matchMap = new LinkedHashMap<>();

        for (int round = 1; round <= losersRounds; round++) {
            int matchesInRound = calculateLosersMatchesInRound(bracketSize, round);
            for (int pos = 0; pos < matchesInRound; pos++) {
                String key = round + "-" + pos;
                matchMap.put(key, Match.builder()
                        .id(UUID.randomUUID())
                        .drawId(draw.getId())
                        .roundNumber(round)
                        .bracketPosition(pos)
                        .build());
            }
        }

        for (int round = 1; round < losersRounds; round++) {
            int matchesInRound = calculateLosersMatchesInRound(bracketSize, round);
            int nextRoundMatches = calculateLosersMatchesInRound(bracketSize, round + 1);

            for (int pos = 0; pos < matchesInRound; pos++) {
                String currentKey = round + "-" + pos;
                int nextPos = pos / 2;
                if (nextPos >= nextRoundMatches) {
                    nextPos = nextRoundMatches - 1;
                }
                String nextKey = (round + 1) + "-" + nextPos;

                Match current = matchMap.get(currentKey);
                Match next = matchMap.get(nextKey);
                if (current != null && next != null) {
                    matchMap.put(currentKey, current.toBuilder().nextMatch(next).build());
                }
            }
        }

        List<Match> matches = new ArrayList<>();
        for (int round = 1; round <= losersRounds; round++) {
            int matchesInRound = calculateLosersMatchesInRound(bracketSize, round);
            for (int pos = 0; pos < matchesInRound; pos++) {
                Match match = matchMap.get(round + "-" + pos);
                if (match != null) {
                    matches.add(match);
                }
            }
        }

        return matches;
    }

    private int calculateLosersMatchesInRound(int bracketSize, int round) {
        int winnersRounds = calculateRounds(bracketSize);

        if (round == 2 * (winnersRounds - 1)) {
            return 1;
        }

        boolean isDropDownRound = round % 2 == 1;

        if (isDropDownRound) {
            int winnersRoundFed = (round + 1) / 2;
            return bracketSize / (int) Math.pow(2, winnersRoundFed + 1);
        } else {
            int prevRoundMatches = calculateLosersMatchesInRound(bracketSize, round - 1);
            return prevRoundMatches / 2;
        }
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
}
