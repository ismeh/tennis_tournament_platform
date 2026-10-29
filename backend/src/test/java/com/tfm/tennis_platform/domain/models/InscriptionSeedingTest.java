package com.tfm.tennis_platform.domain.models;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InscriptionSeedingTest {

    @Test
    void getSeedingPositionReturnsSeedWhenPresent() {
        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .seed(5)
                .points(10)
                .build();

        assertThat(inscription.getSeedingPosition()).isEqualTo(5);
    }

    @Test
    void getSeedingPositionReturnsPointsWhenSeedIsNull() {
        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .seed(null)
                .points(10)
                .build();

        assertThat(inscription.getSeedingPosition()).isEqualTo(10);
    }

    @Test
    void getSeedingPositionReturnsPointsWhenSeedIsZero() {
        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .seed(0)
                .points(10)
                .build();

        assertThat(inscription.getSeedingPosition()).isEqualTo(10);
    }

    @Test
    void getSeedingPositionReturnsNullWhenBothAreNull() {
        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .seed(null)
                .points(null)
                .build();

        assertThat(inscription.getSeedingPosition()).isNull();
    }

    @Test
    void getSeedingPositionReturnsNullWhenBothAreZero() {
        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .seed(0)
                .points(0)
                .build();

        assertThat(inscription.getSeedingPosition()).isNull();
    }
}
