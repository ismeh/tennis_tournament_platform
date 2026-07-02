package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SetScore {
    private final int setNumber;
    private final int firstPlayerGames;
    private final int secondPlayerGames;
    private final Integer firstPlayerTiebreak;
    private final Integer secondPlayerTiebreak;

    public boolean isComplete(boolean isDecisiveSet, int decisiveTiebreakPoints) {
        // Standard sets win at 6 games with a difference of at least 2 games (6-0 to 6-4)
        if (firstPlayerGames == 6 && secondPlayerGames <= 4) {
            return true;
        }
        if (secondPlayerGames == 6 && firstPlayerGames <= 4) {
            return true;
        }

        // Win at 7-5
        if (firstPlayerGames == 7 && secondPlayerGames == 5) {
            return true;
        }
        if (secondPlayerGames == 7 && firstPlayerGames == 5) {
            return true;
        }

        // Tiebreak set (7-6 or 6-7)
        if ((firstPlayerGames == 7 && secondPlayerGames == 6) || (firstPlayerGames == 6 && secondPlayerGames == 7)) {
            if (firstPlayerTiebreak == null || secondPlayerTiebreak == null) {
                return false; // Tiebreak points must be filled
            }

            int targetPoints = isDecisiveSet ? decisiveTiebreakPoints : 7;
            
            // Check tiebreak winner conditions (at least targetPoints and difference >= 2)
            if (firstPlayerGames == 7) {
                return firstPlayerTiebreak >= targetPoints && (firstPlayerTiebreak - secondPlayerTiebreak) >= 2;
            } else {
                return secondPlayerTiebreak >= targetPoints && (secondPlayerTiebreak - firstPlayerTiebreak) >= 2;
            }
        }

        return false;
    }

    public Integer getWinnerSide(boolean isDecisiveSet, int decisiveTiebreakPoints) {
        if (!isComplete(isDecisiveSet, decisiveTiebreakPoints)) {
            return null;
        }
        if (firstPlayerGames > secondPlayerGames) {
            return 1;
        } else if (secondPlayerGames > firstPlayerGames) {
            return 2;
        }
        return null;
    }

    public boolean isTiebreak() {
        return (firstPlayerGames == 7 && secondPlayerGames == 6) || (firstPlayerGames == 6 && secondPlayerGames == 7);
    }
}
