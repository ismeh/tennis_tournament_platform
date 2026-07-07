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
        return isComplete(isDecisiveSet, decisiveTiebreakPoints, 6);
    }

    public boolean isComplete(boolean isDecisiveSet, int decisiveTiebreakPoints, int gamesPerSet) {
        int gamesToWin = gamesPerSet;
        int maxLoss = gamesToWin - 2;

        if (firstPlayerGames == gamesToWin && secondPlayerGames <= maxLoss) {
            return true;
        }
        if (secondPlayerGames == gamesToWin && firstPlayerGames <= maxLoss) {
            return true;
        }

        if (firstPlayerGames == gamesToWin + 1 && secondPlayerGames == gamesToWin - 1) {
            return true;
        }
        if (secondPlayerGames == gamesToWin + 1 && firstPlayerGames == gamesToWin - 1) {
            return true;
        }

        if ((firstPlayerGames == gamesToWin + 1 && secondPlayerGames == gamesToWin)
            || (firstPlayerGames == gamesToWin && secondPlayerGames == gamesToWin + 1)) {
            if (firstPlayerTiebreak == null || secondPlayerTiebreak == null) {
                return false;
            }

            int targetPoints = isDecisiveSet ? decisiveTiebreakPoints : 7;

            if (firstPlayerGames == gamesToWin + 1) {
                return firstPlayerTiebreak >= targetPoints && (firstPlayerTiebreak - secondPlayerTiebreak) >= 2;
            } else {
                return secondPlayerTiebreak >= targetPoints && (secondPlayerTiebreak - firstPlayerTiebreak) >= 2;
            }
        }

        return false;
    }

    public Integer getWinnerSide(boolean isDecisiveSet, int decisiveTiebreakPoints) {
        return getWinnerSide(isDecisiveSet, decisiveTiebreakPoints, 6);
    }

    public Integer getWinnerSide(boolean isDecisiveSet, int decisiveTiebreakPoints, int gamesPerSet) {
        if (!isComplete(isDecisiveSet, decisiveTiebreakPoints, gamesPerSet)) {
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
        return isTiebreak(6);
    }

    public boolean isTiebreak(int gamesPerSet) {
        int gamesToWin = gamesPerSet;
        return (firstPlayerGames == gamesToWin + 1 && secondPlayerGames == gamesToWin)
            || (firstPlayerGames == gamesToWin && secondPlayerGames == gamesToWin + 1);
    }
}
