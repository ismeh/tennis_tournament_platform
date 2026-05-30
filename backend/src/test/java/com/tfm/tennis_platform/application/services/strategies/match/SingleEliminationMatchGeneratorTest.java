package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleEliminationMatchGeneratorTest {

    @Test
    void seeds_first_round_inscriptions_correctly() {
        SingleEliminationMatchGenerator generator = new SingleEliminationMatchGenerator();

        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.ELIMINATION)
                .build();

        List<Inscription> inscriptions = List.of(
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build()
        );

        List<Match> matches = generator.generateMatches(draw, inscriptions);

        int firstRoundMatches = (int) matches.stream().filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 1).count();
        assertEquals(4, firstRoundMatches);

        int placed = matches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 1)
                .mapToInt(m -> (m.getFirstInscriptionId() != null ? 1 : 0) + (m.getSecondInscriptionId() != null ? 1 : 0))
                .sum();

        assertEquals(inscriptions.size(), placed);

        int secondRoundPlaced = matches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 2)
                .mapToInt(m -> (m.getFirstInscriptionId() != null ? 1 : 0) + (m.getSecondInscriptionId() != null ? 1 : 0))
                .sum();

        assertEquals(3, secondRoundPlaced);
    }

    @Test
    void advances_single_first_round_bye_for_seven_inscriptions() {
        SingleEliminationMatchGenerator generator = new SingleEliminationMatchGenerator();

        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.ELIMINATION)
                .build();

        List<Inscription> inscriptions = List.of(
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription()
        );

        List<Match> matches = generator.generateMatches(draw, inscriptions);

        List<Match> firstRoundMatches = matches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 1)
                .toList();
        List<Match> secondRoundMatches = matches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 2)
                .toList();

        assertEquals(4, firstRoundMatches.size());
        assertEquals(2, secondRoundMatches.size());
        assertEquals(1, firstRoundMatches.stream().filter(m -> m.getFirstInscriptionId() != null && m.getSecondInscriptionId() == null).count());
        assertTrue(secondRoundMatches.stream().anyMatch(m -> m.getFirstInscriptionId() != null || m.getSecondInscriptionId() != null));
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .registeredAt(LocalDateTime.now())
                .build();
    }
}
