package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                    .bracketPosition(matchIndex)
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
        List<Inscription> bracketSlots = buildBracketSlots(inscriptions, bracketSize);
        for (int matchIndex = 0; matchIndex < matchesInFirstRound; matchIndex++) {
            String position = 1 + "-" + matchIndex;
            Match existing = matchMap.get(position);
            if (existing == null) {
                continue;
            }

            Inscription firstInscription = bracketSlots.get(matchIndex * 2);
            Inscription secondInscription = bracketSlots.get(matchIndex * 2 + 1);
            Inscription winner = firstInscription != null && secondInscription == null
                    ? firstInscription
                    : firstInscription == null && secondInscription != null ? secondInscription : null;

            matchMap.put(position, existing.toBuilder()
                    .firstInscription(firstInscription)
                    .secondInscription(secondInscription)
                    .winner(winner)
                    .build());
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

    private List<Inscription> buildBracketSlots(List<Inscription> inscriptions, int bracketSize) {
        if (!hasSeedingData(inscriptions)) {
            return buildSequentialBracketSlots(inscriptions, bracketSize);
        }

        List<Inscription> slots = new ArrayList<>();
        for (int index = 0; index < bracketSize; index++) {
            slots.add(null);
        }

        List<Inscription> orderedInscriptions = orderForSeeding(inscriptions);
        List<Integer> seedPositions = calculateSeedPositions(bracketSize);
        int byeCount = bracketSize - inscriptions.size();
        Set<Integer> reservedByeSlots = new HashSet<>();
        int inscriptionIndex = 0;

        for (; inscriptionIndex < byeCount; inscriptionIndex++) {
            int slot = seedPositions.get(inscriptionIndex);
            slots.set(slot, orderedInscriptions.get(inscriptionIndex));
            reservedByeSlots.add(opponentSlot(slot));
        }

        for (; inscriptionIndex < orderedInscriptions.size(); inscriptionIndex++) {
            Inscription inscription = orderedInscriptions.get(inscriptionIndex);
            int slot = firstAvailableSeedSlot(seedPositions, slots, reservedByeSlots);
            slots.set(slot, inscription);
        }

        return slots;
    }

    private int firstAvailableSeedSlot(List<Integer> seedPositions, List<Inscription> slots, Set<Integer> reservedByeSlots) {
        for (Integer slot : seedPositions) {
            if (slots.get(slot) == null && !reservedByeSlots.contains(slot)) {
                return slot;
            }
        }

        for (int slot = 0; slot < slots.size(); slot++) {
            if (slots.get(slot) == null && !reservedByeSlots.contains(slot)) {
                return slot;
            }
        }

        throw new IllegalStateException("No available bracket slot found.");
    }

    private List<Inscription> buildSequentialBracketSlots(List<Inscription> inscriptions, int bracketSize) {
        List<Inscription> slots = new ArrayList<>();
        for (int index = 0; index < bracketSize; index++) {
            slots.add(null);
        }

        int byeCount = bracketSize - inscriptions.size();
        int inscriptionIndex = 0;
        List<Integer> seedPositions = calculateSeedPositions(bracketSize);
        Set<Integer> reservedByeSlots = new HashSet<>();

        for (; inscriptionIndex < byeCount; inscriptionIndex++) {
            int slot = seedPositions.get(inscriptionIndex);
            slots.set(slot, inscriptions.get(inscriptionIndex));
            reservedByeSlots.add(opponentSlot(slot));
        }

        for (; inscriptionIndex < inscriptions.size(); inscriptionIndex++) {
            int slot = firstAvailableSeedSlot(seedPositions, slots, reservedByeSlots);
            slots.set(slot, inscriptions.get(inscriptionIndex));
        }

        return slots;
    }

    private boolean hasSeedingData(List<Inscription> inscriptions) {
        return inscriptions.stream()
                .anyMatch(inscription -> inscription.getSeedingPosition() != null 
                        || (inscription.getPoints() != null && inscription.getPoints() > 0));
    }

    private List<Inscription> orderForSeeding(List<Inscription> inscriptions) {
        return inscriptions.stream()
                .sorted(Comparator
                        .comparing((Inscription inscription) -> inscription.getSeed() == null ? Integer.MAX_VALUE : inscription.getSeed())
                        .thenComparing((Inscription inscription) -> inscription.getPoints() == null ? Integer.MIN_VALUE : inscription.getPoints(), Comparator.reverseOrder())
                        .thenComparing(inscription -> inscription.getRegisteredAt() == null ? java.time.LocalDateTime.MAX : inscription.getRegisteredAt())
                        .thenComparing(Inscription::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Integer> calculateSeedPositions(int bracketSize) {
        List<Integer> positions = new ArrayList<>();
        positions.add(0);

        int size = 2;
        while (size <= bracketSize) {
            List<Integer> expanded = new ArrayList<>();
            for (Integer position : positions) {
                expanded.add(position);
                expanded.add(size - 1 - position);
            }
            positions = expanded;
            size *= 2;
        }

        return positions;
    }

    private int opponentSlot(int slot) {
        return slot % 2 == 0 ? slot + 1 : slot - 1;
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
