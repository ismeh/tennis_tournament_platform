package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolationMatchGeneratorTest {

    private final ConsolationMatchGenerator generator = new ConsolationMatchGenerator();

    @Test
    void generatesBracketForFirstRoundLosers() {
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, eightInscriptions());

        assertThat(matches).isNotEmpty();
        assertThat(matches).allMatch(m -> m.getDrawId().equals(draw.getId()));
        assertThat(matches).allMatch(m -> m.getRoundNumber() != null && m.getRoundNumber() >= 1);
        assertThat(matches).allMatch(m -> m.getBracketPosition() != null);
    }

    @Test
    void secondRoundMatchesLinkToFinalMatch() {
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, eightInscriptions());

        long secondRoundCount = matches.stream()
                .filter(m -> m.getRoundNumber() == 2)
                .count();

        if (secondRoundCount > 0) {
            matches.stream()
                    .filter(m -> m.getRoundNumber() == 1)
                    .forEach(m -> assertThat(m.getNextMatch()).isNotNull());
        }
    }

    @Test
    void emptyListProducesNoMatches() {
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, List.of());

        assertThat(matches).isEmpty();
    }

    @Test
    void singleInscriptionProducesNoMatches() {
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, List.of(inscription()));

        assertThat(matches).isEmpty();
    }

    @Test
    void matchPositionsAreSequential() {
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, eightInscriptions());

        List<Match> round1 = matches.stream()
                .filter(m -> m.getRoundNumber() == 1)
                .toList();

        for (int i = 0; i < round1.size(); i++) {
            assertThat(round1.get(i).getBracketPosition()).isEqualTo(i);
        }
    }

    @Test
    void allMatchesHaveUniqueIds() {
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.CONSOLATION)
                .build();

        List<Match> matches = generator.generateMatches(draw, eightInscriptions());

        long uniqueIds = matches.stream().map(Match::getId).distinct().count();
        assertThat(uniqueIds).isEqualTo(matches.size());
    }

    private List<Inscription> eightInscriptions() {
        return List.of(
                inscription(), inscription(), inscription(), inscription(),
                inscription(), inscription(), inscription(), inscription()
        );
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .build();
    }
}
