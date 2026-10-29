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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DoubleEliminationMatchGenerator")
class DoubleEliminationMatchGeneratorTest {

    private final DoubleEliminationMatchGenerator generator = new DoubleEliminationMatchGenerator();

    @Test
    @DisplayName("8 players should produce 5 losers bracket matches")
    void shouldGenerateCorrectLosersBracketForEightPlayers() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();
        List<Inscription> players = eightPlayers();

        List<Match> matches = generator.generateMatches(draw, players);

        assertEquals(5, matches.size(), "8 players: 3 winners rounds -> losersRounds=4 -> 5 total matches");
        assertTrue(matches.stream().allMatch(m -> m.getRoundNumber() >= 1 && m.getRoundNumber() <= 4));
    }

    @Test
    @DisplayName("4 players should produce 2 losers bracket matches")
    void shouldGenerateCorrectLosersBracketForFourPlayers() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();
        List<Inscription> players = List.of(
                inscription(), inscription(), inscription(), inscription()
        );

        List<Match> matches = generator.generateMatches(draw, players);

        assertEquals(2, matches.size(), "4 players: 2 winners rounds -> losersRounds=2 -> 2 matches (round1=1, round2=final=1)");
        assertEquals(1, matches.get(0).getRoundNumber());
        assertEquals(2, matches.get(1).getRoundNumber());
    }

    @Test
    @DisplayName("2 players should produce 0 losers bracket matches")
    void shouldReturnEmptyForTwoPlayers() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();

        List<Match> matches = generator.generateMatches(draw, List.of(inscription(), inscription()));

        assertTrue(matches.isEmpty(), "2 players: winnersRounds=1 -> losersRounds=0");
    }

    @Test
    @DisplayName("1 player should produce 0 matches")
    void shouldReturnEmptyForSinglePlayer() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();

        List<Match> matches = generator.generateMatches(draw, List.of(inscription()));

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("Final losers round should always have exactly 1 match")
    void finalLosersRoundShouldHaveOneMatch() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();
        List<Inscription> players = eightPlayers();

        List<Match> matches = generator.generateMatches(draw, players);

        int maxRound = matches.stream().mapToInt(Match::getRoundNumber).max().orElse(0);
        long finalRoundCount = matches.stream().filter(m -> m.getRoundNumber() == maxRound).count();
        assertEquals(1, finalRoundCount, "Final losers bracket round has exactly 1 match");
    }

    @Test
    @DisplayName("All matches should reference the correct draw ID")
    void shouldAssignCorrectDrawId() {
        UUID drawId = UUID.randomUUID();
        Draw draw = Draw.builder().id(drawId).drawType(DrawType.DOUBLE_ELIMINATION).build();

        List<Match> matches = generator.generateMatches(draw, eightPlayers());

        assertTrue(matches.stream().allMatch(m -> drawId.equals(m.getDrawId())));
    }

    @Test
    @DisplayName("Matches should be linked via nextMatch within losers bracket")
    void shouldLinkMatchesWithinLosersBracket() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();
        List<Inscription> players = eightPlayers();

        List<Match> matches = generator.generateMatches(draw, players);

        List<Match> nonFinalMatches = matches.stream()
                .filter(m -> m.getRoundNumber() < 4)
                .toList();

        for (Match match : nonFinalMatches) {
            assertNotNull(match.getNextMatch(), "Non-final match should have a next match linked");
        }

        List<Match> finalMatches = matches.stream()
                .filter(m -> m.getRoundNumber() == 4)
                .toList();
        assertEquals(1, finalMatches.size());
    }

    @Test
    @DisplayName("16 players should produce 11 losers bracket matches")
    void shouldGenerateCorrectLosersBracketForSixteenPlayers() {
        Draw draw = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.DOUBLE_ELIMINATION).build();
        List<Inscription> players = sixteenPlayers();

        List<Match> matches = generator.generateMatches(draw, players);

        assertEquals(11, matches.size(), "16 players: 4 winners rounds -> losersRounds=6 -> 11 matches");
        int maxRound = matches.stream().mapToInt(Match::getRoundNumber).max().orElse(0);
        assertEquals(6, maxRound, "6 losers bracket rounds");
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .registeredAt(LocalDateTime.now())
                .build();
    }

    private List<Inscription> eightPlayers() {
        return List.of(
                inscription(), inscription(), inscription(), inscription(),
                inscription(), inscription(), inscription(), inscription()
        );
    }

    private List<Inscription> sixteenPlayers() {
        return List.of(
                inscription(), inscription(), inscription(), inscription(),
                inscription(), inscription(), inscription(), inscription(),
                inscription(), inscription(), inscription(), inscription(),
                inscription(), inscription(), inscription(), inscription()
        );
    }
}
