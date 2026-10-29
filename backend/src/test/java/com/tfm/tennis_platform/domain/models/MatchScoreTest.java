package com.tfm.tennis_platform.domain.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchScoreTest {

    @Test
    void should_complete_match_best_of_3_with_2_sets() {
        SetScore set1 = SetScore.builder().setNumber(1).firstPlayerGames(6).secondPlayerGames(4).build();
        SetScore set2 = SetScore.builder().setNumber(2).firstPlayerGames(6).secondPlayerGames(3).build();

        MatchScore score = MatchScore.builder().sets(List.of(set1, set2)).build();

        assertTrue(score.isMatchComplete(3, 7));
        assertEquals(1, score.getWinningSide(3, 7));
        assertEquals("6-4 6-3", score.toResultString());
    }

    @Test
    void should_complete_match_best_of_3_with_3_sets() {
        SetScore set1 = SetScore.builder().setNumber(1).firstPlayerGames(6).secondPlayerGames(4).build();
        SetScore set2 = SetScore.builder().setNumber(2).firstPlayerGames(3).secondPlayerGames(6).build();
        SetScore set3 = SetScore.builder().setNumber(3).firstPlayerGames(7).secondPlayerGames(5).build();

        MatchScore score = MatchScore.builder().sets(List.of(set1, set2, set3)).build();

        assertTrue(score.isMatchComplete(3, 7));
        assertEquals(1, score.getWinningSide(3, 7));
        assertEquals("6-4 3-6 7-5", score.toResultString());
    }

    @Test
    void should_complete_match_best_of_5() {
        SetScore set1 = SetScore.builder().setNumber(1).firstPlayerGames(6).secondPlayerGames(4).build();
        SetScore set2 = SetScore.builder().setNumber(2).firstPlayerGames(3).secondPlayerGames(6).build();
        SetScore set3 = SetScore.builder().setNumber(3).firstPlayerGames(7).secondPlayerGames(5).build();
        SetScore set4 = SetScore.builder().setNumber(4).firstPlayerGames(6).secondPlayerGames(1).build();

        MatchScore score = MatchScore.builder().sets(List.of(set1, set2, set3, set4)).build();

        assertTrue(score.isMatchComplete(5, 7));
        assertEquals(1, score.getWinningSide(5, 7));
        assertEquals("6-4 3-6 7-5 6-1", score.toResultString());
    }

    @Test
    void should_parse_standard_results_correctly() {
        MatchScore parsed = MatchScore.fromResultString("6-4 3-6 7-6(5)");
        assertNotNull(parsed.getSets());
        assertEquals(3, parsed.getSets().size());

        SetScore set1 = parsed.getSets().get(0);
        assertEquals(1, set1.getSetNumber());
        assertEquals(6, set1.getFirstPlayerGames());
        assertEquals(4, set1.getSecondPlayerGames());

        SetScore set3 = parsed.getSets().get(2);
        assertEquals(3, set3.getSetNumber());
        assertEquals(7, set3.getFirstPlayerGames());
        assertEquals(6, set3.getSecondPlayerGames());
        assertEquals(7, set3.getFirstPlayerTiebreak());
        assertEquals(5, set3.getSecondPlayerTiebreak());
    }
}
