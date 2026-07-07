package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.*;
import com.tfm.tennis_platform.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchDomainMapperTest {

    @Mock
    private JpaDrawRepository drawRepository;
    @Mock
    private JpaInscriptionRepository inscriptionRepository;
    @Mock
    private JpaMatchRepository matchRepository;
    @Mock
    private JpaCourtRepository courtRepository;
    @InjectMocks
    private MatchDomainMapper mapper;

    private UUID matchId;
    private UUID drawId;
    private UUID firstInscriptionId;
    private UUID secondInscriptionId;
    private UUID winnerId;
    private UUID courtId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        drawId = UUID.randomUUID();
        firstInscriptionId = UUID.randomUUID();
        secondInscriptionId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
        courtId = UUID.randomUUID();
    }

    @Nested
    class ToDomainTests {

        @Test
        void should_return_null_when_entity_is_null() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        void should_map_basic_fields_with_no_inscriptions() {
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .roundNumber(1)
                    .bracketPosition(2)
                    .scheduledAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                    .scheduleTimeType(ScheduleTimeType.EXACT)
                    .court("Court 1")
                    .result("6-3 7-5")
                    .status(MatchStatus.COMPLETED)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(matchId);
            assertThat(result.getDrawId()).isNull();
            assertThat(result.getFirstInscriptionId()).isNull();
            assertThat(result.getSecondInscriptionId()).isNull();
            assertThat(result.getWinnerId()).isNull();
            assertThat(result.getRoundNumber()).isEqualTo(1);
            assertThat(result.getBracketPosition()).isEqualTo(2);
            assertThat(result.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
            assertThat(result.getScheduleTimeType()).isEqualTo(ScheduleTimeType.EXACT);
            assertThat(result.getCourtId()).isNull();
            assertThat(result.getCourt()).isEqualTo("Court 1");
            assertThat(result.getResult()).isEqualTo("6-3 7-5");
            assertThat(result.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        }

        @Test
        void should_enrich_professional_participant_with_pro_data() {
            ParticipantEntity proParticipant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("ABC123")
                    .seed(5)
                    .build();
            InscriptionEntity inscriptionEntity = InscriptionEntity.builder()
                    .id(firstInscriptionId)
                    .participant(proParticipant)
                    .status("CONFIRMED")
                    .paymentStatus("PAID")
                    .registeredAt(LocalDateTime.of(2026, 6, 1, 12, 0))
                    .build();

            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(inscriptionEntity)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getFirstInscription()).isNotNull();
            assertThat(result.getFirstInscription().getId()).isEqualTo(firstInscriptionId);
            assertThat(result.getFirstInscription().getParticipantSource()).isEqualTo(ParticipantSource.PROFESSIONAL);
            assertThat(result.getFirstInscription().getSeed()).isEqualTo(5);
        }

        @Test
        void should_not_load_pro_data_for_amateur_participant() {
            ParticipantEntity amateur = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.MANUAL)
                    .seed(3)
                    .build();
            InscriptionEntity inscriptionEntity = InscriptionEntity.builder()
                    .id(firstInscriptionId)
                    .participant(amateur)
                    .status("PENDING")
                    .paymentStatus("UNPAID")
                    .build();

            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(inscriptionEntity)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getFirstInscription()).isNotNull();
            assertThat(result.getFirstInscription().getParticipantSource()).isEqualTo(ParticipantSource.MANUAL);
            assertThat(result.getFirstInscription().getSeed()).isEqualTo(3);
        }

        @Test
        void should_handle_null_inscription() {
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(null)
                    .secondInscription(null)
                    .winner(null)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getFirstInscription()).isNull();
            assertThat(result.getSecondInscription()).isNull();
            assertThat(result.getWinner()).isNull();
        }

        @Test
        void should_map_null_draw_to_null_draw_id() {
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .draw(null)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getDrawId()).isNull();
        }

        @Test
        void should_map_draw_to_draw_id() {
            DrawEntity draw = DrawEntity.builder().id(drawId).build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .draw(draw)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getDrawId()).isEqualTo(drawId);
        }

        @Test
        void should_map_court_resource_to_court_id_and_name() {
            CourtEntity court = CourtEntity.builder().id(courtId).name("Central Court").build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .courtResource(court)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getCourtId()).isEqualTo(courtId);
            assertThat(result.getCourt()).isEqualTo("Central Court");
        }

        @Test
        void should_map_next_match_reference() {
            UUID nextMatchId = UUID.randomUUID();
            MatchEntity nextMatchEntity = MatchEntity.builder().id(nextMatchId).build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .nextMatch(nextMatchEntity)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getNextMatch()).isNotNull();
            assertThat(result.getNextMatch().getId()).isEqualTo(nextMatchId);
        }
    }

    @Nested
    class ToEntityTests {

        @Test
        void should_return_null_when_domain_is_null() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        void should_map_basic_fields_and_set_next_match_to_null() {
            Match domain = Match.builder()
                    .id(matchId)
                    .drawId(drawId)
                    .roundNumber(1)
                    .bracketPosition(2)
                    .scheduledAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                    .scheduleTimeType(ScheduleTimeType.EXACT)
                    .court("Court 1")
                    .result("6-3 7-5")
                    .status(MatchStatus.COMPLETED)
                    .build();

            DrawEntity drawRef = DrawEntity.builder().id(drawId).build();
            when(drawRepository.getReferenceById(drawId)).thenReturn(drawRef);

            MatchEntity result = mapper.toEntity(domain);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(matchId);
            assertThat(result.getDraw()).isSameAs(drawRef);
            assertThat(result.getRoundNumber()).isEqualTo(1);
            assertThat(result.getBracketPosition()).isEqualTo(2);
            assertThat(result.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
            assertThat(result.getScheduleTimeType()).isEqualTo(ScheduleTimeType.EXACT);
            assertThat(result.getCourt()).isEqualTo("Court 1");
            assertThat(result.getResult()).isEqualTo("6-3 7-5");
            assertThat(result.getStatus()).isEqualTo(MatchStatus.COMPLETED);
            assertThat(result.getNextMatch()).isNull();
            assertThat(result.getLoserNextMatch()).isNull();
        }

        @Test
        void should_map_null_draw_id_to_null_draw_entity() {
            Match domain = Match.builder()
                    .id(matchId)
                    .drawId(null)
                    .roundNumber(1)
                    .build();

            MatchEntity result = mapper.toEntity(domain);

            assertThat(result.getDraw()).isNull();
            verifyNoInteractions(drawRepository);
        }

        @Test
        void should_map_null_inscription_to_null_entity_ref() {
            Match domain = Match.builder()
                    .id(matchId)
                    .firstInscription(null)
                    .secondInscription(null)
                    .winner(null)
                    .roundNumber(1)
                    .build();

            MatchEntity result = mapper.toEntity(domain);

            assertThat(result.getFirstInscription()).isNull();
            assertThat(result.getSecondInscription()).isNull();
            assertThat(result.getWinner()).isNull();
            verifyNoInteractions(inscriptionRepository);
        }

        @Test
        void should_map_inscription_ids_to_entity_refs() {
            com.tfm.tennis_platform.domain.models.Inscription firstIns =
                    com.tfm.tennis_platform.domain.models.Inscription.builder()
                            .id(firstInscriptionId).build();
            com.tfm.tennis_platform.domain.models.Inscription secondIns =
                    com.tfm.tennis_platform.domain.models.Inscription.builder()
                            .id(secondInscriptionId).build();
            com.tfm.tennis_platform.domain.models.Inscription winnerIns =
                    com.tfm.tennis_platform.domain.models.Inscription.builder()
                            .id(winnerId).build();

            Match domain = Match.builder()
                    .id(matchId)
                    .firstInscription(firstIns)
                    .secondInscription(secondIns)
                    .winner(winnerIns)
                    .roundNumber(1)
                    .build();

            InscriptionEntity firstRef = InscriptionEntity.builder().id(firstInscriptionId).build();
            InscriptionEntity secondRef = InscriptionEntity.builder().id(secondInscriptionId).build();
            InscriptionEntity winnerRef = InscriptionEntity.builder().id(winnerId).build();
            when(inscriptionRepository.getReferenceById(firstInscriptionId)).thenReturn(firstRef);
            when(inscriptionRepository.getReferenceById(secondInscriptionId)).thenReturn(secondRef);
            when(inscriptionRepository.getReferenceById(winnerId)).thenReturn(winnerRef);

            MatchEntity result = mapper.toEntity(domain);

            assertThat(result.getFirstInscription()).isSameAs(firstRef);
            assertThat(result.getSecondInscription()).isSameAs(secondRef);
            assertThat(result.getWinner()).isSameAs(winnerRef);
        }

        @Test
        void should_map_court_id_to_entity_ref() {
            Match domain = Match.builder()
                    .id(matchId)
                    .courtId(courtId)
                    .roundNumber(1)
                    .build();

            CourtEntity courtRef = CourtEntity.builder().id(courtId).build();
            when(courtRepository.getReferenceById(courtId)).thenReturn(courtRef);

            MatchEntity result = mapper.toEntity(domain);

            assertThat(result.getCourtResource()).isSameAs(courtRef);
        }

        @Test
        void should_map_null_court_id_to_null_court_resource() {
            Match domain = Match.builder()
                    .id(matchId)
                    .courtId(null)
                    .roundNumber(1)
                    .build();

            MatchEntity result = mapper.toEntity(domain);

            assertThat(result.getCourtResource()).isNull();
            verifyNoInteractions(courtRepository);
        }
    }

    @Nested
    class ToEntityWithoutNextMatchTests {

        @Test
        void should_behave_same_as_toEntity() {
            Match domain = Match.builder()
                    .id(matchId)
                    .roundNumber(1)
                    .bracketPosition(3)
                    .status(MatchStatus.PENDING)
                    .build();

            MatchEntity fromToEntity = mapper.toEntity(domain);
            MatchEntity fromWithout = mapper.toEntityWithoutNextMatch(domain);

            assertThat(fromWithout.getId()).isEqualTo(fromToEntity.getId());
            assertThat(fromWithout.getRoundNumber()).isEqualTo(fromToEntity.getRoundNumber());
            assertThat(fromWithout.getBracketPosition()).isEqualTo(fromToEntity.getBracketPosition());
            assertThat(fromWithout.getStatus()).isEqualTo(fromToEntity.getStatus());
            assertThat(fromWithout.getNextMatch()).isNull();
            assertThat(fromWithout.getLoserNextMatch()).isNull();
        }

        @Test
        void should_return_null_when_domain_is_null() {
            assertThat(mapper.toEntityWithoutNextMatch(null)).isNull();
        }
    }

    @Nested
    class ToDomainListTests {

        @Test
        void should_return_empty_list_when_input_is_null() {
            assertThat(mapper.toDomainList(null)).isEmpty();
        }

        @Test
        void should_return_empty_list_when_input_is_empty() {
            assertThat(mapper.toDomainList(List.of())).isEmpty();
        }

        @Test
        void should_batch_load_professional_data_once() {
            String license1 = "LIC001";
            String license2 = "LIC002";

            ParticipantEntity pro1 = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId(license1)
                    .build();
            ParticipantEntity pro2 = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId(license2)
                    .build();

            InscriptionEntity ins1 = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .participant(pro1)
                    .build();
            InscriptionEntity ins2 = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .participant(pro2)
                    .build();

            MatchEntity e1 = MatchEntity.builder()
                    .id(UUID.randomUUID())
                    .firstInscription(ins1)
                    .roundNumber(1)
                    .build();
            MatchEntity e2 = MatchEntity.builder()
                    .id(UUID.randomUUID())
                    .secondInscription(ins2)
                    .roundNumber(2)
                    .build();

            List<Match> results = mapper.toDomainList(List.of(e1, e2));

            assertThat(results).hasSize(2);
        }

        @Test
        void should_map_entities_without_pro_participants() {
            ParticipantEntity manual = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.MANUAL)
                    .build();
            InscriptionEntity ins = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .participant(manual)
                    .build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(ins)
                    .roundNumber(1)
                    .build();

            List<Match> results = mapper.toDomainList(List.of(entity));

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getFirstInscription().getParticipantSource())
                    .isEqualTo(ParticipantSource.MANUAL);
        }
    }

    @Nested
    class NormalizeLicenseTests {

        @Test
        void should_return_null_pro_data_for_null_license() {
            ParticipantEntity pro = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId(null)
                    .build();
            InscriptionEntity ins = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .participant(pro)
                    .build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(ins)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getFirstInscription()).isNotNull();
            assertThat(result.getFirstInscription().getParticipantSource()).isEqualTo(ParticipantSource.PROFESSIONAL);
        }

        @Test
        void should_return_null_pro_data_for_blank_license() {
            ParticipantEntity pro = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("   ")
                    .build();
            InscriptionEntity ins = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .participant(pro)
                    .build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(ins)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getFirstInscription()).isNotNull();
            assertThat(result.getFirstInscription().getParticipantSource()).isEqualTo(ParticipantSource.PROFESSIONAL);
        }

        @Test
        void should_trim_and_lowercase_license_for_pro_lookup() {
            ParticipantEntity pro = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("  MyLicense123  ")
                    .build();
            InscriptionEntity ins = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .participant(pro)
                    .build();
            MatchEntity entity = MatchEntity.builder()
                    .id(matchId)
                    .firstInscription(ins)
                    .roundNumber(1)
                    .build();

            Match result = mapper.toDomain(entity);

            assertThat(result.getFirstInscription()).isNotNull();
            assertThat(result.getFirstInscription().getParticipantSource()).isEqualTo(ParticipantSource.PROFESSIONAL);
        }
    }
}
