package com.tfm.tennis_platform.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetScoreTest {

    @Test
    void should_be_complete_with_standard_scores() {
        // 6-4
        SetScore set1 = SetScore.builder().setNumber(1).firstPlayerGames(6).secondPlayerGames(4).build();
        assertTrue(set1.isComplete(false, 7));
        assertEquals(1, set1.getWinnerSide(false, 7));

        // 2-6
        SetScore set2 = SetScore.builder().setNumber(1).firstPlayerGames(2).secondPlayerGames(6).build();
        assertTrue(set2.isComplete(false, 7));
        assertEquals(2, set2.getWinnerSide(false, 7));
    }

    @Test
    void should_not_be_complete_when_games_too_low() {
        // 5-4
        SetScore set1 = SetScore.builder().setNumber(1).firstPlayerGames(5).secondPlayerGames(4).build();
        assertFalse(set1.isComplete(false, 7));

        // 6-5
        SetScore set2 = SetScore.builder().setNumber(1).firstPlayerGames(6).secondPlayerGames(5).build();
        assertFalse(set2.isComplete(false, 7));
    }

    @Test
    void should_handle_7_5_scores() {
        // 7-5
        SetScore set1 = SetScore.builder().setNumber(1).firstPlayerGames(7).secondPlayerGames(5).build();
        assertTrue(set1.isComplete(false, 7));
        assertEquals(1, set1.getWinnerSide(false, 7));

        // 5-7
        SetScore set2 = SetScore.builder().setNumber(1).firstPlayerGames(5).secondPlayerGames(7).build();
        assertTrue(set2.isComplete(false, 7));
        assertEquals(2, set2.getWinnerSide(false, 7));
    }

    @Test
    void should_handle_tiebreaks() {
        // 7-6(5)
        SetScore set1 = SetScore.builder()
                .setNumber(1)
                .firstPlayerGames(7)
                .secondPlayerGames(6)
                .firstPlayerTiebreak(7)
                .secondPlayerTiebreak(5)
                .build();
        assertTrue(set1.isComplete(false, 7));
        assertEquals(1, set1.getWinnerSide(false, 7));
        assertTrue(set1.isTiebreak());

        // 6-7(9)
        SetScore set2 = SetScore.builder()
                .setNumber(1)
                .firstPlayerGames(6)
                .secondPlayerGames(7)
                .firstPlayerTiebreak(9)
                .secondPlayerTiebreak(11)
                .build();
        assertTrue(set2.isComplete(false, 7));
        assertEquals(2, set2.getWinnerSide(false, 7));
        assertTrue(set2.isTiebreak());
    }

    @Test
    void should_handle_decisive_super_tiebreak() {
        // Decisive set tiebreak to 10 points
        SetScore set1 = SetScore.builder()
                .setNumber(3)
                .firstPlayerGames(7)
                .secondPlayerGames(6)
                .firstPlayerTiebreak(10)
                .secondPlayerTiebreak(8)
                .build();
        assertTrue(set1.isComplete(true, 10));
        assertEquals(1, set1.getWinnerSide(true, 10));

        // Tiebreak to 10 points not complete at 7 points
        SetScore set2 = SetScore.builder()
                .setNumber(3)
                .firstPlayerGames(7)
                .secondPlayerGames(6)
                .firstPlayerTiebreak(7)
                .secondPlayerTiebreak(5)
                .build();
        assertFalse(set2.isComplete(true, 10));
    }
}
