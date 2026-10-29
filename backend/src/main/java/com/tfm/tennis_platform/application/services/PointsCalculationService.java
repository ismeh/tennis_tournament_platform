package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PointsCalculationService {

    private static final Map<StageType, int[]> POINTS_BY_ROUND = Map.of(
        StageType.MAIN, new int[]{100, 60, 30, 15, 5},
        StageType.CONSOLATION, new int[]{20, 10, 5},
        StageType.QUALIFYING, new int[]{15, 8, 4, 2},
        StageType.PLAYOFF, new int[]{25, 12, 6},
        StageType.ROUND_ROBIN, new int[]{10, 6, 3, 1},
        StageType.DOUBLE_ELIMINATION, new int[]{80, 50, 25, 12, 4}
    );

    private static final int BASE_POINTS_WALKOVER = 5;
    private static final int MAX_OPPONENT_BONUS = 20;
    private static final int MIN_OPPONENT_BONUS = 0;
    private static final int MAX_PARTICIPANTS_REFERENCE = 64;

    public PointsResult calculate(Match match, StageType stageType, int totalParticipantsInStage) {
        boolean isWalkover = match.getStatus() == MatchStatus.WALKOVER;
        boolean isRetired = match.getStatus() == MatchStatus.RETIRED;

        int[] roundPoints = POINTS_BY_ROUND.getOrDefault(stageType, new int[]{10, 5, 2, 1});
        int roundNumber = match.getRoundNumber() != null ? match.getRoundNumber() : 1;
        int roundIndex = Math.min(roundNumber - 1, roundPoints.length - 1);
        int basePoints = roundPoints[roundIndex];

        if (isWalkover || isRetired) {
            return new PointsResult(BASE_POINTS_WALKOVER, BASE_POINTS_WALKOVER);
        }

        int winnerPoints = basePoints + calculateOpponentBonus(match, true, totalParticipantsInStage);
        int loserPoints = calculateLoserPoints(basePoints, roundNumber);

        return new PointsResult(winnerPoints, loserPoints);
    }

    public PointsResult calculateForDynamicMatch(StageType stageType, int roundNumber, int totalParticipantsInStage) {
        int[] roundPoints = POINTS_BY_ROUND.getOrDefault(stageType, new int[]{10, 5, 2, 1});
        int roundIndex = Math.min(roundNumber - 1, roundPoints.length - 1);
        int basePoints = roundPoints[roundIndex];

        int winnerPoints = basePoints + MAX_OPPONENT_BONUS / 2;
        int loserPoints = calculateLoserPoints(basePoints, roundNumber);

        return new PointsResult(winnerPoints, loserPoints);
    }

    private int calculateOpponentBonus(Match match, boolean winnerIsFirst, int totalParticipants) {
        int opponentSeed = resolveOpponentSeed(match, winnerIsFirst);
        if (opponentSeed <= 0) {
            return MAX_OPPONENT_BONUS / 2;
        }

        double seedRatio = 1.0 - ((double) opponentSeed / Math.max(totalParticipants, 1));
        int bonus = (int) Math.round(seedRatio * MAX_OPPONENT_BONUS);
        return Math.max(MIN_OPPONENT_BONUS, Math.min(MAX_OPPONENT_BONUS, bonus));
    }

    private int resolveOpponentSeed(Match match, boolean winnerIsFirst) {
        Inscription opponent = winnerIsFirst ? match.getSecondInscription() : match.getFirstInscription();
        if (opponent == null) {
            return 0;
        }

        Integer seed = opponent.getSeed();
        if (seed != null && seed > 0) {
            return seed;
        }

        Integer localPoints = opponent.getPoints();
        if (localPoints != null && localPoints > 0) {
            return localPoints;
        }

        return 0;
    }

    private int calculateLoserPoints(int basePoints, int roundNumber) {
        if (roundNumber <= 1) {
            return Math.max(1, basePoints / 4);
        }
        return Math.max(1, basePoints / 2);
    }

    public record PointsResult(int winnerPoints, int loserPoints) {}
}
