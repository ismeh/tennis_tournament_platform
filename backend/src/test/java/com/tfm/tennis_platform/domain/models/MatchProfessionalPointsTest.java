package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchProfessionalPointsTest {

    @Test
    void shouldExposeWinPointsFromFirstPlayerPoints() {
        Inscription firstInscription = Inscription.builder()
                .id(UUID.randomUUID())
                .participantSource(ParticipantSource.PROFESSIONAL)
                .build();
        Inscription secondInscription = Inscription.builder()
                .id(UUID.randomUUID())
                .participantSource(ParticipantSource.PROFESSIONAL)
                .build();

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(firstInscription)
                .secondInscription(secondInscription)
                .firstPlayerPoints("250")
                .secondPlayerPoints("120")
                .build();

        assertThat(match.getFirstWinPoints()).isEqualTo(250);
        assertThat(match.getSecondWinPoints()).isEqualTo(120);
    }

    @Test
    void shouldReturnNullWinPointsWhenNotSet() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(Inscription.builder()
                        .id(UUID.randomUUID())
                        .participantSource(ParticipantSource.EXISTING_PERSON)
                        .build())
                .secondInscription(Inscription.builder()
                        .id(UUID.randomUUID())
                        .participantSource(ParticipantSource.PROFESSIONAL)
                        .build())
                .build();

        assertThat(match.getFirstWinPoints()).isNull();
        assertThat(match.getSecondWinPoints()).isNull();
    }
}
