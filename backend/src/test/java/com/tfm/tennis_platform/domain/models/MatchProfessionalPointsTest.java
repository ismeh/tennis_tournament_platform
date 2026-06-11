package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchProfessionalPointsTest {

    @Test
    void shouldExposeWinPointsWhenBothPlayersAreProfessional() {
        Inscription firstInscription = professionalInscription(120);
        Inscription secondInscription = professionalInscription(250);

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(firstInscription)
                .secondInscription(secondInscription)
                .build();

        assertThat(match.isProfessionalMatch()).isTrue();
        assertThat(match.getFirstWinPoints()).isEqualTo(250);
        assertThat(match.getSecondWinPoints()).isEqualTo(120);
    }

    @Test
    void shouldNotExposeWinPointsWhenAnyPlayerIsNotProfessional() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .firstInscription(professionalInscription(120))
                .secondInscription(Inscription.builder()
                        .id(UUID.randomUUID())
                        .participantSource(ParticipantSource.EXISTING_PERSON)
                        .build())
                .build();

        assertThat(match.isProfessionalMatch()).isFalse();
        assertThat(match.getFirstWinPoints()).isNull();
        assertThat(match.getSecondWinPoints()).isNull();
    }

    private Inscription professionalInscription(Integer awardedPoints) {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .participantSource(ParticipantSource.PROFESSIONAL)
                .professionalAwardedPoints(awardedPoints)
                .build();
    }
}
