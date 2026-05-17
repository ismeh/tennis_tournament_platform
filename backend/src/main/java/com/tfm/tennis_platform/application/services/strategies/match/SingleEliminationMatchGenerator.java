package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, UUID> matchPositions = new HashMap<>();
        Map<String, Match> matchMap = new HashMap<>();

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

        allMatches.addAll(matchMap.values());
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
