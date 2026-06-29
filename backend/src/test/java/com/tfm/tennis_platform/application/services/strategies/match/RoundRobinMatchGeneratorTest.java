package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RoundRobinMatchGenerator")
class RoundRobinMatchGeneratorTest {

    private final RoundRobinMatchGenerator generator = new RoundRobinMatchGenerator();

    @Test
    @DisplayName("4 players should produce 6 matches in 3 rounds (N*(N-1)/2)")
    void shouldGenerateCorrectNumberOfMatchesForFourPlayers() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();
        List<Inscription> players = List.of(
                inscription(), inscription(), inscription(), inscription()
        );

        List<Match> matches = generator.generateMatches(draw, players);

        assertEquals(6, matches.size(), "4 players produce 6 round-robin matches");
        long round1 = matches.stream().filter(m -> m.getRoundNumber() == 1).count();
        long round2 = matches.stream().filter(m -> m.getRoundNumber() == 2).count();
        long round3 = matches.stream().filter(m -> m.getRoundNumber() == 3).count();
        assertEquals(2, round1, "Round 1 has 2 matches");
        assertEquals(2, round2, "Round 2 has 2 matches");
        assertEquals(2, round3, "Round 3 has 2 matches");
    }

    @Test
    @DisplayName("3 players should produce 3 matches (odd count uses bye slot)")
    void shouldHandleOddNumberOfPlayersWithBye() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();
        List<Inscription> players = List.of(
                inscription(), inscription(), inscription()
        );

        List<Match> matches = generator.generateMatches(draw, players);

        assertEquals(3, matches.size(), "3 players produce 3 round-robin matches");
        assertTrue(matches.stream().allMatch(m -> m.getRoundNumber() > 0), "All matches have round numbers");
    }

    @Test
    @DisplayName("2 players should produce 1 match in 1 round")
    void shouldGenerateOneMatchForTwoPlayers() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();
        List<Inscription> players = List.of(inscription(), inscription());

        List<Match> matches = generator.generateMatches(draw, players);

        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).getRoundNumber());
    }

    @Test
    @DisplayName("1 player should produce 0 matches")
    void shouldReturnEmptyForSinglePlayer() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();

        List<Match> matches = generator.generateMatches(draw, List.of(inscription()));

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("No player should face themselves")
    void shouldNeverPairSamePlayerAgainstThemselves() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();
        Inscription p1 = inscription();
        Inscription p2 = inscription();
        Inscription p3 = inscription();
        Inscription p4 = inscription();

        List<Match> matches = generator.generateMatches(draw, List.of(p1, p2, p3, p4));

        for (Match match : matches) {
            assertTrue(!match.getFirstInscriptionId().equals(match.getSecondInscriptionId()),
                    "Player should not face themselves in " + match);
        }
    }

    @Test
    @DisplayName("Every player should play every other player exactly once")
    void shouldPairEachPlayerExactlyOnceAgainstEachOpponent() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();
        Inscription p1 = inscription();
        Inscription p2 = inscription();
        Inscription p3 = inscription();
        Inscription p4 = inscription();

        List<Match> matches = generator.generateMatches(draw, List.of(p1, p2, p3, p4));

        List<UUID> playerIds = List.of(p1.getId(), p2.getId(), p3.getId(), p4.getId());
        int pairCount = 0;
        for (int i = 0; i < playerIds.size(); i++) {
            for (int j = i + 1; j < playerIds.size(); j++) {
                UUID idA = playerIds.get(i);
                UUID idB = playerIds.get(j);
                boolean found = matches.stream().anyMatch(m ->
                        (m.getFirstInscriptionId().equals(idA) && m.getSecondInscriptionId().equals(idB)) ||
                        (m.getFirstInscriptionId().equals(idB) && m.getSecondInscriptionId().equals(idA))
                );
                assertTrue(found, "Players " + i + " and " + j + " should play each other");
                pairCount++;
            }
        }
        assertEquals(6, pairCount, "All pairs should be covered");
    }

    @Test
    @DisplayName("All matches should reference the correct draw ID")
    void shouldAssignCorrectDrawId() {
        UUID drawId = UUID.randomUUID();
        Draw draw = Draw.builder().id(drawId).drawType(DrawType.ROUND_ROBIN).build();

        List<Match> matches = generator.generateMatches(draw, List.of(inscription(), inscription(), inscription()));

        assertTrue(matches.stream().allMatch(m -> drawId.equals(m.getDrawId())));
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .registeredAt(LocalDateTime.now())
                .build();
    }
}
