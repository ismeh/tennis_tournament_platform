package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchLocalPointsTest {

    @Test
    void shouldReturnParsedPointsWhenSetAsStrings() {
        Inscription first = Inscription.builder()
                .id(UUID.randomUUID())
                .participantSource(ParticipantSource.EXISTING_PERSON)
                .build();
        Inscription second = Inscription.builder()
                .id(UUID.randomUUID())
                .participantSource(ParticipantSource.EXISTING_PERSON)
                .build();

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(first)
                .secondInscription(second)
                .firstPlayerPoints("100")
                .secondPlayerPoints("30")
                .status(MatchStatus.COMPLETED)
                .build();

        assertThat(match.getFirstWinPoints()).isEqualTo(100);
        assertThat(match.getSecondWinPoints()).isEqualTo(30);
    }

    @Test
    void shouldReturnNullPointsWhenNotSet() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.COMPLETED)
                .build();

        assertThat(match.getFirstWinPoints()).isNull();
        assertThat(match.getSecondWinPoints()).isNull();
    }

    @Test
    void shouldDetectCompletedNormally() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.COMPLETED)
                .build();

        assertThat(match.isCompletedNormally()).isTrue();
        assertThat(match.isWalkoverOrRetired()).isFalse();
    }

    @Test
    void shouldDetectWalkoverOrRetired() {
        Match walkover = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.WALKOVER)
                .build();
        Match retired = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.RETIRED)
                .build();

        assertThat(walkover.isWalkoverOrRetired()).isTrue();
        assertThat(retired.isWalkoverOrRetired()).isTrue();
    }
}
