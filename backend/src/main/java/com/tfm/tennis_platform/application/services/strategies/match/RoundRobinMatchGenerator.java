package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class RoundRobinMatchGenerator implements MatchGenerationStrategy {

    private static final int DEFAULT_GROUP_SIZE = 4;

    @Override
    public List<Match> generateMatches(Draw draw, List<Inscription> inscriptions) {
        List<Inscription> groupPlayers = extractGroupPlayers(draw, inscriptions);
        if (groupPlayers.size() < 2) {
            return List.of();
        }

        int n = groupPlayers.size();
        boolean hasBye = n % 2 != 0;
        int playerCount = hasBye ? n + 1 : n;
        int totalRounds = playerCount - 1;
        int matchesPerRound = playerCount / 2;

        List<Inscription> slots = new ArrayList<>(groupPlayers);
        if (hasBye) {
            slots.add(null);
        }

        List<Match> matches = new ArrayList<>();

        for (int round = 0; round < totalRounds; round++) {
            for (int i = 0; i < matchesPerRound; i++) {
                Inscription first = slots.get(i);
                Inscription second = slots.get(playerCount - 1 - i);

                if (first == null || second == null) {
                    continue;
                }

                matches.add(Match.builder()
                        .id(UUID.randomUUID())
                        .drawId(draw.getId())
                        .firstInscription(first)
                        .secondInscription(second)
                        .roundNumber(round + 1)
                        .bracketPosition(i)
                        .build());
            }

            if (playerCount > 2) {
                Inscription last = slots.remove(playerCount - 1);
                slots.add(1, last);
            }
        }

        return matches;
    }

    private List<Inscription> extractGroupPlayers(Draw draw, List<Inscription> allInscriptions) {
        Integer groupIndex = draw.getGroupIndex();
        if (groupIndex == null) {
            return allInscriptions;
        }

        List<Inscription> sorted = allInscriptions.stream()
                .sorted(Comparator
                        .comparing((Inscription ins) -> ins.getSeedingPosition() == null ? Integer.MAX_VALUE : ins.getSeedingPosition())
                        .thenComparing(ins -> ins.getRegisteredAt() == null ? java.time.LocalDateTime.MAX : ins.getRegisteredAt())
                        .thenComparing(Inscription::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int groupCount = (int) Math.ceil((double) sorted.size() / DEFAULT_GROUP_SIZE);
        int groupSize = (int) Math.ceil((double) sorted.size() / groupCount);
        int fromIndex = groupIndex * groupSize;
        int toIndex = Math.min(fromIndex + groupSize, sorted.size());

        if (fromIndex >= sorted.size()) {
            return List.of();
        }

        return sorted.subList(fromIndex, toIndex);
    }
}
