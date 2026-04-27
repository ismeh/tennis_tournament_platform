package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.enums.ParticipantType;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionsResponse;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaEventRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

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

    @InjectMocks
    private InscriptionService inscriptionService;

    @Test
    void should_return_tournament_inscriptions_with_category_and_gender_counters() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        RefAgeCategoryEntity absoluteCategory = RefAgeCategoryEntity.builder()
                .id(1)
                .category("Absoluto")
                .description("Absoluto")
                .build();

        EventEntity event = EventEntity.builder()
                .id(eventId)
                .ageCategory(absoluteCategory)
                .name("Absoluto - Masculino")
                .gender("MALE")
                .build();

        PersonEntity singlesPlayer = PersonEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Carlos")
                .lastName("Lopez")
                .gender("MALE")
                .birthDate(LocalDate.of(1995, 2, 10))
                .build();

        PersonEntity pairPlayerOne = PersonEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Ana")
                .lastName("Ruiz")
                .gender("FEMALE")
                .birthDate(LocalDate.of(1997, 4, 1))
                .build();

        PersonEntity pairPlayerTwo = PersonEntity.builder()
                .id(UUID.randomUUID())
                .firstName("Luis")
                .lastName("Martin")
                .gender("MALE")
                .birthDate(LocalDate.of(1993, 7, 20))
                .build();

        ParticipantEntity individualParticipant = ParticipantEntity.builder()
                .id(UUID.randomUUID())
                .participantType(ParticipantType.INDIVIDUAL)
                .individualPerson(singlesPlayer)
                .members(List.of(singlesPlayer))
                .build();

        ParticipantEntity pairParticipant = ParticipantEntity.builder()
                .id(UUID.randomUUID())
                .participantType(ParticipantType.PAIR)
                .members(List.of(pairPlayerOne, pairPlayerTwo))
                .build();

        InscriptionEntity singlesInscription = InscriptionEntity.builder()
                .id(UUID.randomUUID())
                .event(event)
                .participant(individualParticipant)
                .registeredAt(LocalDateTime.of(2026, 4, 20, 10, 0))
                .build();

        InscriptionEntity pairInscription = InscriptionEntity.builder()
                .id(UUID.randomUUID())
                .event(event)
                .participant(pairParticipant)
                .registeredAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                .build();

        when(tournamentRepository.existsById(tournamentId)).thenReturn(true);
        when(eventRepository.findAllByTournamentId(tournamentId)).thenReturn(List.of(event));
        when(inscriptionRepository.findDetailedByTournamentIdAndEventId(tournamentId, null))
                .thenReturn(List.of(singlesInscription, pairInscription));

        TournamentInscriptionsResponse response = inscriptionService.findByTournament(tournamentId, null);

        assertEquals(1, response.events().size());
        assertEquals(3, response.inscriptions().size());
        assertEquals("Carlos", response.inscriptions().get(0).firstName());
        assertEquals("Absoluto", response.categoryCounts().get(0).category());
        assertEquals(3L, response.categoryCounts().get(0).totalPlayers());
        assertEquals(2L, response.categoryCounts().get(0).genders().stream()
                .filter(counter -> "MALE".equals(counter.gender()))
                .findFirst()
                .orElseThrow()
                .totalPlayers());
        assertEquals(1L, response.categoryCounts().get(0).genders().stream()
                .filter(counter -> "FEMALE".equals(counter.gender()))
                .findFirst()
                .orElseThrow()
                .totalPlayers());
    }

    @Test
    void should_return_empty_inscriptions_for_tournament_without_entries() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        RefAgeCategoryEntity under18Category = RefAgeCategoryEntity.builder()
                .id(2)
                .category("Sub-18")
                .description("Sub-18")
                .build();

        EventEntity event = EventEntity.builder()
                .id(eventId)
                .ageCategory(under18Category)
                .name("Sub-18 - Femenino")
                .gender("FEMALE")
                .build();

        when(tournamentRepository.existsById(tournamentId)).thenReturn(true);
        when(eventRepository.findAllByTournamentId(tournamentId)).thenReturn(List.of(event));
        when(inscriptionRepository.findDetailedByTournamentIdAndEventId(tournamentId, null)).thenReturn(List.of());

        TournamentInscriptionsResponse response = inscriptionService.findByTournament(tournamentId, null);

        assertEquals(0, response.inscriptions().size());
        assertEquals(1, response.categoryCounts().size());
        assertEquals(0L, response.categoryCounts().get(0).totalPlayers());
    }

    @Test
    void should_throw_when_event_filter_does_not_belong_to_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(tournamentRepository.existsById(tournamentId)).thenReturn(true);
        when(eventRepository.findByIdAndTournament_Id(eventId, tournamentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inscriptionService.findByTournament(tournamentId, eventId)
        );

        assertEquals("Event not found for tournament", exception.getMessage());
    }
}
