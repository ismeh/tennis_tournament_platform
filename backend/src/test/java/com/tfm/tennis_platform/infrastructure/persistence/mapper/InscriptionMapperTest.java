package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscriptionMapperTest {

    @Mock
    private JpaProPlayerRepository proPlayerRepository;
    @InjectMocks
    private InscriptionMapper mapper;

    @Nested
    class ToDomainTests {
        @Test
        void should_return_null_when_entity_is_null() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        void should_map_basic_fields() {
            UUID eventId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            EventEntity event = EventEntity.builder().id(eventId).build();
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(participantId)
                    .participantSource(ParticipantSource.EXISTING_PERSON)
                    .seed(5)
                    .build();

            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .participant(participant)
                    .status("CONFIRMED")
                    .paymentStatus("PAID")
                    .registeredAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                    .build();

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getEventId()).isEqualTo(eventId);
            assertThat(result.getParticipantId()).isEqualTo(participantId);
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(result.getPaymentStatus()).isEqualTo("PAID");
            assertThat(result.getRegisteredAt()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0));
            assertThat(result.getParticipantSource()).isEqualTo(ParticipantSource.EXISTING_PERSON);
            assertThat(result.getSeed()).isEqualTo(5);
        }

        @Test
        void should_resolve_professional_ranking_position() {
            UUID participantId = UUID.randomUUID();
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(participantId)
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("L001")
                    .build();

            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(EventEntity.builder().id(UUID.randomUUID()).build())
                    .participant(participant)
                    .build();

            ProPlayerEntity proPlayer = ProPlayerEntity.builder().rankingPosition(3).build();
            when(proPlayerRepository.findFirstByLicenseIgnoreCase("L001")).thenReturn(Optional.of(proPlayer));

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getProfessionalRankingPosition()).isEqualTo(3);
        }

        @Test
        void should_resolve_professional_awarded_points() {
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("L001")
                    .build();

            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(EventEntity.builder().id(UUID.randomUUID()).build())
                    .participant(participant)
                    .build();

            ProPlayerEntity proPlayer = ProPlayerEntity.builder().awardedPoints(100).build();
            when(proPlayerRepository.findFirstByLicenseIgnoreCase("L001")).thenReturn(Optional.of(proPlayer));

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getProfessionalAwardedPoints()).isEqualTo(100);
        }

        @Test
        void should_return_null_ranking_when_not_professional() {
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.EXISTING_PERSON)
                    .build();

            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(EventEntity.builder().id(UUID.randomUUID()).build())
                    .participant(participant)
                    .build();

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getProfessionalRankingPosition()).isNull();
            assertThat(result.getProfessionalAwardedPoints()).isNull();
            verifyNoInteractions(proPlayerRepository);
        }

        @Test
        void should_return_null_ranking_when_license_is_blank() {
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("   ")
                    .build();

            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(EventEntity.builder().id(UUID.randomUUID()).build())
                    .participant(participant)
                    .build();

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getProfessionalRankingPosition()).isNull();
        }

        @Test
        void should_return_null_ranking_when_pro_player_not_found() {
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantSource(ParticipantSource.PROFESSIONAL)
                    .displayTennisId("L999")
                    .build();

            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(EventEntity.builder().id(UUID.randomUUID()).build())
                    .participant(participant)
                    .build();

            when(proPlayerRepository.findFirstByLicenseIgnoreCase("L999")).thenReturn(Optional.empty());

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getProfessionalRankingPosition()).isNull();
        }

        @Test
        void should_handle_null_participant() {
            InscriptionEntity entity = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .event(EventEntity.builder().id(UUID.randomUUID()).build())
                    .participant(null)
                    .build();

            Inscription result = mapper.toDomain(entity);

            assertThat(result.getParticipantId()).isNull();
            assertThat(result.getParticipantSource()).isNull();
            assertThat(result.getSeed()).isNull();
        }
    }

    @Nested
    class ToEntityTests {
        @Test
        void should_return_null_when_domain_is_null() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        void should_map_basic_fields() {
            UUID eventId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            Inscription domain = Inscription.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .participantId(participantId)
                    .status("PENDING")
                    .paymentStatus("UNPAID")
                    .registeredAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                    .build();

            InscriptionEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            assertThat(result.getEvent().getId()).isEqualTo(eventId);
            assertThat(result.getParticipant().getId()).isEqualTo(participantId);
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getPaymentStatus()).isEqualTo("UNPAID");
            assertThat(result.getRegisteredAt()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0));
        }

        @Test
        void should_handle_null_event_id() {
            Inscription domain = Inscription.builder()
                    .id(UUID.randomUUID())
                    .status("PENDING")
                    .build();

            InscriptionEntity result = mapper.toEntity(domain);

            assertThat(result.getEvent()).isNull();
            assertThat(result.getParticipant()).isNull();
        }
    }
}
