package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentRepositoryAdapterTest {

    @Mock
    private JpaTournamentRepository tournamentJpaRepository;

    @Mock
    private TournamentEntityMapper mapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TournamentRepositoryAdapter repositoryAdapter;

    @Test
    void should_reuse_managed_event_entities_when_full_list_is_sent_again() {
        UUID tournamentId = UUID.randomUUID();
        UUID existingEventId = UUID.randomUUID();
        UUID newEventId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity existingEventEntity = EventEntity.builder()
                .id(existingEventId)
                .gender("MALE")
                .tournament(tournamentEntity)
                .build();
        tournamentEntity.getEvents().add(existingEventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder().id(existingEventId).categoryId(1).gender("FEMALE").build(),
                        Event.builder().id(newEventId).categoryId(2).gender("MIXED").build()
                ))
                .build();

        RefAgeCategoryEntity categoryOne = RefAgeCategoryEntity.builder().id(1).build();
        RefAgeCategoryEntity categoryTwo = RefAgeCategoryEntity.builder().id(2).build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(categoryOne);
        when(entityManager.getReference(RefAgeCategoryEntity.class, 2)).thenReturn(categoryTwo);
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        Tournament savedTournament = repositoryAdapter.save(domainTournament);

        EventEntity reusedEventEntity = tournamentEntity.getEvents().stream()
                .filter(eventEntity -> existingEventId.equals(eventEntity.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(2, tournamentEntity.getEvents().size());
        assertSame(existingEventEntity, reusedEventEntity);
        assertEquals("FEMALE", reusedEventEntity.getGender());
        assertSame(categoryOne, reusedEventEntity.getAgeCategory());
        verify(mapper).updateEntityFromDomain(domainTournament, tournamentEntity);
        assertSame(domainTournament, savedTournament);
    }

    @Test
    void should_update_status_when_saving_existing_tournament() {
        UUID tournamentId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .status(TournamentStatus.DRAFT)
                .events(new ArrayList<>())
                .build();

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.OPEN)
                .events(List.of())
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        Tournament savedTournament = repositoryAdapter.save(domainTournament);

        verify(mapper).updateEntityFromDomain(domainTournament, tournamentEntity);
        assertSame(domainTournament, savedTournament);
    }
}
