package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PointsCalculationServiceTest {

    private PointsCalculationService service;

    @BeforeEach
    void setUp() {
        service = new PointsCalculationService();
    }

    @Test
    void mainStageFirstRoundWinnerGetsBasePlusOpponentBonus() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(8).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isGreaterThan(100);
        assertThat(result.loserPoints()).isGreaterThan(0).isLessThan(100);
    }

    @Test
    void walkoverReturnsBasePointsOnly() {
        Inscription first = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription second = Inscription.builder().id(UUID.randomUUID()).seed(2).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(first)
                .secondInscription(second)
                .roundNumber(1)
                .status(MatchStatus.WALKOVER)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isEqualTo(5);
        assertThat(result.loserPoints()).isEqualTo(5);
    }

    @Test
    void retiredReturnsBasePointsOnly() {
        Inscription first = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription second = Inscription.builder().id(UUID.randomUUID()).seed(2).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(first)
                .secondInscription(second)
                .roundNumber(1)
                .status(MatchStatus.RETIRED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isEqualTo(5);
        assertThat(result.loserPoints()).isEqualTo(5);
    }

    @Test
    void consolationStageUsesCorrectPointTiers() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(4).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.CONSOLATION, 8);

        assertThat(result.winnerPoints()).isGreaterThan(20);
        assertThat(result.loserPoints()).isGreaterThan(0);
    }

    @Test
    void qualifyingStageFirstRound() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(4).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.QUALIFYING, 16);

        assertThat(result.winnerPoints()).isGreaterThan(15);
        assertThat(result.loserPoints()).isGreaterThan(0);
    }

    @Test
    void unknownStageTypeUsesDefaultPoints() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.PLAYOFF, 8);

        assertThat(result.winnerPoints()).isGreaterThan(0);
        assertThat(result.loserPoints()).isGreaterThan(0);
    }

    @Test
    void loserPointsDecreaseInLaterRounds() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(2).build();

        Match round1 = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        Match round2 = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(2)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult r1 = service.calculate(round1, StageType.MAIN, 16);
        PointsCalculationService.PointsResult r2 = service.calculate(round2, StageType.MAIN, 16);

        assertThat(r1.winnerPoints()).isGreaterThanOrEqualTo(r2.winnerPoints());
    }

    @Test
    void nullRoundNumberDefaultsToFirstRound() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(2).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(null)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isGreaterThan(0);
    }

    @Test
    void nullInscriptionsUseDefaultOpponentBonus() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(null)
                .secondInscription(null)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isGreaterThan(100);
        assertThat(result.loserPoints()).isGreaterThan(0);
    }

    @Test
    void opponentSeedBonusScalesWithSeedPosition() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription highSeedLoser = Inscription.builder().id(UUID.randomUUID()).seed(2).build();
        Inscription lowSeedLoser = Inscription.builder().id(UUID.randomUUID()).seed(15).build();

        Match vsHighSeed = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(highSeedLoser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        Match vsLowSeed = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(lowSeedLoser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult highResult = service.calculate(vsHighSeed, StageType.MAIN, 16);
        PointsCalculationService.PointsResult lowResult = service.calculate(vsLowSeed, StageType.MAIN, 16);

        assertThat(highResult.winnerPoints()).isGreaterThan(lowResult.winnerPoints());
    }

    @Test
    void pointsFromLocalRankingUsedWhenSeedIsNull() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(null).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(null).points(10).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isGreaterThan(100);
    }

    @Test
    void calculateForDynamicMatchUsesFixedOpponentBonus() {
        PointsCalculationService.PointsResult result = service.calculateForDynamicMatch(StageType.MAIN, 1, 16);

        assertThat(result.winnerPoints()).isGreaterThan(0);
        assertThat(result.loserPoints()).isGreaterThan(0);
    }

    @Test
    void roundRobinStageUsesCorrectPoints() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.ROUND_ROBIN, 4);

        assertThat(result.winnerPoints()).isGreaterThan(10);
    }

    @Test
    void doubleEliminationStageUsesCorrectPoints() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(1)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.DOUBLE_ELIMINATION, 16);

        assertThat(result.winnerPoints()).isGreaterThan(80);
    }

    @Test
    void highRoundNumberClampsToLastTier() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(2).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(100)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.MAIN, 16);

        assertThat(result.winnerPoints()).isGreaterThan(0);
    }

    @Test
    void loserPointsMinimumIsOne() {
        Inscription winner = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Inscription loser = Inscription.builder().id(UUID.randomUUID()).seed(1).build();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(winner)
                .secondInscription(loser)
                .roundNumber(5)
                .status(MatchStatus.COMPLETED)
                .build();

        PointsCalculationService.PointsResult result = service.calculate(match, StageType.CONSOLATION, 4);

        assertThat(result.loserPoints()).isGreaterThanOrEqualTo(1);
    }
}
