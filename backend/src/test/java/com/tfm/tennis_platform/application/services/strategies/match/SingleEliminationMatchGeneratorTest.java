package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
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

    @Test
    void distributes_seeded_players_across_opposite_halves() {
        SingleEliminationMatchGenerator generator = new SingleEliminationMatchGenerator();
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.ELIMINATION)
                .build();

        Inscription firstSeed = seededInscription(1);
        Inscription secondSeed = seededInscription(2);
        Inscription thirdSeed = seededInscription(3);
        Inscription fourthSeed = seededInscription(4);

        List<Match> matches = generator.generateMatches(draw, List.of(
                fourthSeed,
                inscription(),
                secondSeed,
                inscription(),
                firstSeed,
                inscription(),
                thirdSeed,
                inscription()
        ));

        List<Match> firstRoundMatches = matches.stream()
                .filter(match -> match.getRoundNumber() != null && match.getRoundNumber() == 1)
                .toList();

        assertEquals(firstSeed.getId(), firstRoundMatches.get(0).getFirstInscriptionId());
        assertEquals(thirdSeed.getId(), firstRoundMatches.get(1).getSecondInscriptionId());
        assertEquals(fourthSeed.getId(), firstRoundMatches.get(2).getFirstInscriptionId());
        assertEquals(secondSeed.getId(), firstRoundMatches.get(3).getSecondInscriptionId());
    }

    @Test
    void assigns_byes_to_best_ranked_professional_players() {
        SingleEliminationMatchGenerator generator = new SingleEliminationMatchGenerator();
        Draw draw = Draw.builder()
                .id(UUID.randomUUID())
                .drawType(DrawType.ELIMINATION)
                .build();

        Inscription firstRanked = professionalInscription(1);
        Inscription secondRanked = professionalInscription(2);
        Inscription thirdRanked = professionalInscription(3);

        List<Match> matches = generator.generateMatches(draw, List.of(
                inscription(),
                thirdRanked,
                secondRanked,
                firstRanked,
                inscription()
        ));

        List<Match> byeMatches = matches.stream()
                .filter(match -> match.getRoundNumber() != null && match.getRoundNumber() == 1)
                .filter(match -> (match.getFirstInscriptionId() == null) != (match.getSecondInscriptionId() == null))
                .toList();

        assertEquals(3, byeMatches.size());
        assertTrue(byeMatches.stream().anyMatch(match -> firstRanked.getId().equals(match.getWinnerId())));
        assertTrue(byeMatches.stream().anyMatch(match -> secondRanked.getId().equals(match.getWinnerId())));
        assertTrue(byeMatches.stream().anyMatch(match -> thirdRanked.getId().equals(match.getWinnerId())));
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .registeredAt(LocalDateTime.now())
                .build();
    }

    private Inscription seededInscription(Integer seed) {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .seed(seed)
                .registeredAt(LocalDateTime.now())
                .build();
    }

    private Inscription professionalInscription(Integer rankingPosition) {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .participantSource(ParticipantSource.PROFESSIONAL)
                .professionalRankingPosition(rankingPosition)
                .registeredAt(LocalDateTime.now())
                .build();
    }
}
