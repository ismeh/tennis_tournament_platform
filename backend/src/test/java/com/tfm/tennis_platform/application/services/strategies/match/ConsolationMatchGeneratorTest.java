package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsolationMatchGeneratorTest {

    @Test
    void creates_empty_bracket_for_first_round_losers() {
        ConsolationMatchGenerator generator = new ConsolationMatchGenerator();
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, List.of(
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription(),
                inscription()
        ));

        assertEquals(3, matches.size());
        assertEquals(2, matches.stream().filter(match -> match.getRoundNumber() == 1).count());
        assertEquals(1, matches.stream().filter(match -> match.getRoundNumber() == 2).count());
        matches.forEach(match -> {
            assertNull(match.getFirstInscriptionId());
            assertNull(match.getSecondInscriptionId());
        });
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .build();
    }
}
