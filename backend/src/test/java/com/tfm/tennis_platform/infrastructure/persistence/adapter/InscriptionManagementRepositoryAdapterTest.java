package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.ManualEventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.ProPlayerRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaEventRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscriptionManagementRepositoryAdapter - Business Validation Tests")
class InscriptionManagementRepositoryAdapterTest {

    @Mock
    private JpaTournamentRepository tournamentRepository;
    @Mock
    private JpaEventRepository eventRepository;
    @Mock
    private JpaInscriptionRepository inscriptionRepository;
    @Mock
    private JpaMemberRepository memberRepository;
    @Mock
    private JpaPersonRepository personRepository;
    @Mock
    private JpaParticipantRepository participantRepository;
    @Mock
    private ProPlayerRepository proPlayerRepository;

    @InjectMocks
    private InscriptionManagementRepositoryAdapter adapter;

    private UUID tournamentId;
    private UUID eventId;
    private UUID memberId;
    private UUID personId;
    private TournamentEntity openTournament;
    private EventEntity event;
    private MemberEntity member;
    private PersonEntity person;

    @BeforeEach
    void setUp() {
        tournamentId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        personId = UUID.randomUUID();

        RefAgeCategoryEntity ageCategory = new RefAgeCategoryEntity();
        ageCategory.setId(8);
        ageCategory.setCategory("Absoluta");

        openTournament = TournamentEntity.builder()
                .id(tournamentId)
                .status(TournamentStatus.OPEN)
                .build();

        event = EventEntity.builder()
                .id(eventId)
                .tournament(openTournament)
                .ageCategory(ageCategory)
                .gender("MALE")
                .build();

        person = PersonEntity.builder()
                .id(personId)
                .firstName("Carlos")
                .lastName("Garcia")
                .gender("MALE")
                .birthDate(LocalDate.of(1990, 5, 15))
                .build();

        member = MemberEntity.builder()
                .id(memberId)
                .email("test@test.com")
                .personId(personId)
                .build();
    }

    @Nested
    @DisplayName("register() - Self-service inscription validations")
    class RegisterTests {

        @Test
        @DisplayName("Should reject when tournament does not exist")
        void shouldRejectWhenTournamentNotFound() {
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se encontró el torneo");
        }

        @Test
        @DisplayName("Should reject when tournament status is not OPEN")
        void shouldRejectWhenTournamentNotOpen() {
            openTournament.setStatus(TournamentStatus.DRAFT);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo están permitidas cuando el torneo está abierto");
        }

        @Test
        @DisplayName("Should reject when tournament status is CLOSED")
        void shouldRejectWhenTournamentClosed() {
            openTournament.setStatus(TournamentStatus.CLOSED);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo están permitidas cuando el torneo está abierto");
        }

        @Test
        @DisplayName("Should reject when tournament status is IN_PROGRESS")
        void shouldRejectWhenTournamentInProgress() {
            openTournament.setStatus(TournamentStatus.IN_PROGRESS);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo están permitidas cuando el torneo está abierto");
        }

        @Test
        @DisplayName("Should reject when event does not exist in tournament")
        void shouldRejectWhenEventNotFound() {
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se encontró el evento");
        }

        @Test
        @DisplayName("Should reject when requester member does not exist")
        void shouldRejectWhenMemberNotFound() {
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se encontró tu cuenta");
        }

        @Test
        @DisplayName("Should reject when requester profile is incomplete (no personId)")
        void shouldRejectWhenProfileIncompleteNoPersonId() {
            member.setPersonId(null);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("test@test.com")).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Completa tu perfil");
        }

        @Test
        @DisplayName("Should reject when person record does not exist")
        void shouldRejectWhenPersonNotFound() {
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("test@test.com")).thenReturn(Optional.of(member));
            when(personRepository.findById(personId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Completa tu perfil");
        }

        @Test
        @DisplayName("Should reject self-partner (partnerId equals member's own ID)")
        void shouldRejectSelfPartner() {
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("test@test.com")).thenReturn(Optional.of(member));
            when(personRepository.findById(personId)).thenReturn(Optional.of(person));

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, memberId), "test@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("compañero distinto a tu propia cuenta");
        }

        @Test
        @DisplayName("Should reject duplicate inscription (same participant + same event)")
        void shouldRejectDuplicateInscription() {
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .build();

            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("test@test.com")).thenReturn(Optional.of(member));
            when(personRepository.findById(personId)).thenReturn(Optional.of(person));
            when(participantRepository.findByTournamentIdAndIndividualPersonId(tournamentId, personId))
                    .thenReturn(Optional.empty());
            when(participantRepository.save(any())).thenReturn(participant);
            when(inscriptionRepository.existsByEvent_IdAndParticipant_Id(eventId, participant.getId())).thenReturn(true);

            assertThatThrownBy(() -> adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya está inscrito en el evento");
        }

        @Test
        @DisplayName("Should register successfully with valid data")
        void shouldRegisterSuccessfully() {
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .build();
            InscriptionEntity saved = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .build();

            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("test@test.com")).thenReturn(Optional.of(member));
            when(personRepository.findById(personId)).thenReturn(Optional.of(person));
            when(participantRepository.findByTournamentIdAndIndividualPersonId(tournamentId, personId))
                    .thenReturn(Optional.empty());
            when(participantRepository.save(any())).thenReturn(participant);
            when(inscriptionRepository.existsByEvent_IdAndParticipant_Id(eventId, participant.getId())).thenReturn(false);
            when(inscriptionRepository.save(any())).thenReturn(saved);

            var result = adapter.register(tournamentId, eventId,
                    new EventInscriptionCommand(8, null), "test@test.com");

            assertThat(result).isNotNull();
            verify(inscriptionRepository).save(any(InscriptionEntity.class));
        }
    }

    @Nested
    @DisplayName("registerManual() - Admin manual inscription validations")
    class RegisterManualTests {

        @Test
        @DisplayName("Should reject when tournament does not exist")
        void shouldRejectWhenTournamentNotFound() {
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "Carlos", "Garcia", "MALE",
                            null, null, null, null),
                    "admin@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No se encontró el torneo");
        }

        @Test
        @DisplayName("Should reject when tournament is not OPEN")
        void shouldRejectWhenTournamentNotOpen() {
            openTournament.setStatus(TournamentStatus.DRAFT);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "Carlos", "Garcia", "MALE",
                            null, null, null, null),
                    "admin@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solo están permitidas cuando el torneo está abierto");
        }

        @Test
        @DisplayName("Should reject when requester is not tournament owner")
        void shouldRejectWhenNotTournamentOwner() {
            MemberEntity otherMember = MemberEntity.builder()
                    .id(UUID.randomUUID())
                    .email("other@test.com")
                    .build();
            openTournament.setCreatedBy(member);

            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherMember));

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "Carlos", "Garcia", "MALE",
                            null, null, null, null),
                    "other@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solo el creador del torneo");
        }

        @Test
        @DisplayName("Should reject manual participant with null playerSource")
        void shouldRejectNullPlayerSource() {
            openTournament.setCreatedBy(member);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(null, null, "Carlos", "Garcia", "MALE",
                            null, null, null, null),
                    "admin@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Indica el origen del jugador");
        }

        @Test
        @DisplayName("Should reject manual participant with blank firstName")
        void shouldRejectBlankFirstName() {
            openTournament.setCreatedBy(member);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "", "Garcia", "MALE",
                            null, null, null, null),
                    "admin@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Indica al menos el nombre y el género");
        }

        @Test
        @DisplayName("Should reject manual participant with blank gender")
        void shouldRejectBlankGender() {
            openTournament.setCreatedBy(member);
            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "Carlos", "Garcia", "",
                            null, null, null, null),
                    "admin@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Indica al menos el nombre y el género");
        }

        @Test
        @DisplayName("Should reject duplicate manual inscription")
        void shouldRejectDuplicateManualInscription() {
            openTournament.setCreatedBy(member);
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .participantType(com.tfm.tennis_platform.domain.models.enums.ParticipantType.INDIVIDUAL)
                    .participantSource(ParticipantSource.MANUAL)
                    .displayFirstName("Carlos")
                    .displayLastName("Garcia")
                    .displayGender("MALE")
                    .build();

            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(member));
            when(participantRepository.findByTournamentId(tournamentId)).thenReturn(List.of(participant));
            when(inscriptionRepository.existsByEvent_IdAndParticipant_Id(eventId, participant.getId())).thenReturn(true);

            assertThatThrownBy(() -> adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "Carlos", "Garcia", "MALE",
                            null, null, null, null),
                    "admin@test.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya está inscrito en el evento");
        }

        @Test
        @DisplayName("Should register manual participant successfully")
        void shouldRegisterManualSuccessfully() {
            openTournament.setCreatedBy(member);
            ParticipantEntity participant = ParticipantEntity.builder()
                    .id(UUID.randomUUID())
                    .build();
            InscriptionEntity saved = InscriptionEntity.builder()
                    .id(UUID.randomUUID())
                    .build();

            when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(openTournament));
            when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.of(event));
            when(memberRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(member));
            when(participantRepository.findByTournamentId(tournamentId)).thenReturn(List.of());
            when(participantRepository.save(any())).thenReturn(participant);
            when(inscriptionRepository.existsByEvent_IdAndParticipant_Id(eventId, participant.getId())).thenReturn(false);
            when(inscriptionRepository.save(any())).thenReturn(saved);

            var result = adapter.registerManual(tournamentId, eventId,
                    new ManualEventInscriptionCommand(ParticipantSource.MANUAL, null, "Carlos", "Garcia", "MALE",
                            null, null, null, null),
                    "admin@test.com");

            assertThat(result).isNotNull();
            verify(inscriptionRepository).save(any(InscriptionEntity.class));
        }
    }
}
