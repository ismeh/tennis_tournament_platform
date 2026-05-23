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

class SingleEliminationMatchGeneratorTest {

    @Test
    void seeds_first_round_inscriptions_correctly() {
        SingleEliminationMatchGenerator generator = new SingleEliminationMatchGenerator();

        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.ELIMINATION)
                .build();

        // 5 inscriptions -> 3 matches in first round, one bye
        List<Inscription> inscriptions = List.of(
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build(),
                Inscription.builder().id(UUID.randomUUID()).eventId(UUID.randomUUID()).participantId(UUID.randomUUID()).registeredAt(LocalDateTime.now()).build()
        );

        List<Match> matches = generator.generateMatches(draw, inscriptions);

        // Count matches in first round
        int firstRoundMatches = (int) matches.stream().filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 1).count();
        assertEquals((inscriptions.size() + 1) / 2, firstRoundMatches);

        // Ensure all inscriptions have been placed into first round slots (some second slots may be null for byes)
        int placed = matches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == 1)
                .mapToInt(m -> (m.getFirstInscriptionId() != null ? 1 : 0) + (m.getSecondInscriptionId() != null ? 1 : 0))
                .sum();

        assertEquals(inscriptions.size(), placed);
    }
}
