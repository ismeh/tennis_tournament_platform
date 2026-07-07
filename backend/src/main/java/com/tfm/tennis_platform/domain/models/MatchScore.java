package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Builder(toBuilder = true)
public class MatchScore {
    private final List<SetScore> sets;

    public static MatchScore empty() {
        return MatchScore.builder().sets(new ArrayList<>()).build();
    }

    public boolean isMatchComplete(int setsPerMatch, int decisiveTiebreakPoints) {
        return isMatchComplete(setsPerMatch, decisiveTiebreakPoints, 6);
    }

    public boolean isMatchComplete(int setsPerMatch, int decisiveTiebreakPoints, int gamesPerSet) {
        int setsToWin;
        if (setsPerMatch == 1) {
            setsToWin = 1;
        } else if (setsPerMatch == 2) {
            setsToWin = 2;
        } else if (setsPerMatch == 5) {
            setsToWin = 3;
        } else {
            setsToWin = 2;
        }

        int player1Sets = 0;
        int player2Sets = 0;

        if (sets == null || sets.isEmpty()) {
            return false;
        }

        for (SetScore set : sets) {
            boolean isDecisive = (set.getSetNumber() == setsPerMatch);
            if (!set.isComplete(isDecisive, decisiveTiebreakPoints, gamesPerSet)) {
                return false;
            }
            Integer winner = set.getWinnerSide(isDecisive, decisiveTiebreakPoints, gamesPerSet);
            if (winner != null) {
                if (winner == 1) {
                    player1Sets++;
                } else if (winner == 2) {
                    player2Sets++;
                }
            }

            if (player1Sets == setsToWin || player2Sets == setsToWin) {
                return true;
            }
        }

        return false;
    }

    public Integer getWinningSide(int setsPerMatch, int decisiveTiebreakPoints) {
        return getWinningSide(setsPerMatch, decisiveTiebreakPoints, 6);
    }

    public Integer getWinningSide(int setsPerMatch, int decisiveTiebreakPoints, int gamesPerSet) {
        int setsToWin;
        if (setsPerMatch == 1) {
            setsToWin = 1;
        } else if (setsPerMatch == 2) {
            setsToWin = 2;
        } else if (setsPerMatch == 5) {
            setsToWin = 3;
        } else {
            setsToWin = 2;
        }

        int player1Sets = 0;
        int player2Sets = 0;

        if (sets == null) {
            return null;
        }

        for (SetScore set : sets) {
            boolean isDecisive = (set.getSetNumber() == setsPerMatch);
            if (!set.isComplete(isDecisive, decisiveTiebreakPoints, gamesPerSet)) {
                continue;
            }
            Integer winner = set.getWinnerSide(isDecisive, decisiveTiebreakPoints, gamesPerSet);
            if (winner != null) {
                if (winner == 1) {
                    player1Sets++;
                } else if (winner == 2) {
                    player2Sets++;
                }
            }

            if (player1Sets == setsToWin) {
                return 1;
            }
            if (player2Sets == setsToWin) {
                return 2;
            }
        }

        return null;
    }

    public String toResultString() {
        return toResultString(6);
    }

    public String toResultString(int gamesPerSet) {
        if (sets == null || sets.isEmpty()) {
            return "";
        }

        List<String> setStrings = new ArrayList<>();
        for (SetScore set : sets) {
            if (set.isTiebreak(gamesPerSet)) {
                Integer tb1 = set.getFirstPlayerTiebreak();
                Integer tb2 = set.getSecondPlayerTiebreak();
                int minTb = (tb1 != null && tb2 != null) ? Math.min(tb1, tb2) : 0;
                setStrings.add(set.getFirstPlayerGames() + "-" + set.getSecondPlayerGames() + "(" + minTb + ")");
            } else {
                setStrings.add(set.getFirstPlayerGames() + "-" + set.getSecondPlayerGames());
            }
        }
        return String.join(" ", setStrings);
    }

    /**
     * Parses a string representation like "6-4 3-6 7-6(5)" into a MatchScore object.
     */
    public static MatchScore fromResultString(String result) {
        List<SetScore> parsedSets = new ArrayList<>();
        if (result == null || result.isBlank() || result.equalsIgnoreCase("Walkover") || result.equalsIgnoreCase("Retirada")) {
            return MatchScore.builder().sets(parsedSets).build();
        }

        String[] parts = result.trim().split("\\s+");
        Pattern pattern = Pattern.compile("(\\d+)-(\\d+)(?:\\((\\d+)\\))?");

        for (int i = 0; i < parts.length; i++) {
            Matcher matcher = pattern.matcher(parts[i]);
            if (matcher.matches()) {
                int games1 = Integer.parseInt(matcher.group(1));
                int games2 = Integer.parseInt(matcher.group(2));
                Integer tb1 = null;
                Integer tb2 = null;

                if (matcher.group(3) != null) {
                    int loserTb = Integer.parseInt(matcher.group(3));
                    // Deduce who won the tiebreak based on games
                    if (games1 > games2) {
                        tb1 = Math.max(7, loserTb + 2); // default winner tb score
                        tb2 = loserTb;
                    } else {
                        tb1 = loserTb;
                        tb2 = Math.max(7, loserTb + 2);
                    }
                }
                parsedSets.add(SetScore.builder()
                        .setNumber(i + 1)
                        .firstPlayerGames(games1)
                        .secondPlayerGames(games2)
                        .firstPlayerTiebreak(tb1)
                        .secondPlayerTiebreak(tb2)
                        .build());
            }
        }

        return MatchScore.builder().sets(parsedSets).build();
    }
}
