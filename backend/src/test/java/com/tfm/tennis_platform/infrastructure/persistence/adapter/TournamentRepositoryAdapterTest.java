package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MatchDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentRepositoryAdapterTest {

    @Mock
    private JpaTournamentRepository tournamentJpaRepository;

    @Mock
    private TournamentEntityMapper mapper;

    @Mock
    private MatchDomainMapper matchDomainMapper;

    @Mock
    private JpaMatchRepository matchJpaRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TournamentRepositoryAdapter repositoryAdapter;

    @Test
    @SuppressWarnings("unchecked")
    void should_find_tournament_summaries_without_mapping_full_domain_graph() {
        UUID professionalTournamentId = UUID.randomUUID();
        UUID regularTournamentId = UUID.randomUUID();
        TournamentSummary professionalSummary = new TournamentSummary(
                professionalTournamentId,
                "Open Pro",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 8),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                TournamentStatus.OPEN,
                false
        );
        TournamentSummary regularSummary = new TournamentSummary(
                regularTournamentId,
                "Open Social",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 8),
                LocalTime.of(10, 0),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 20),
                Surface.HARD,
                64,
                "Club Norte",
                TournamentStatus.DRAFT,
                false
        );
        TypedQuery<TournamentSummary> summaryQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> professionalFlagQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(TournamentSummary.class))).thenReturn(summaryQuery);
        when(summaryQuery.getResultList()).thenReturn(List.of(professionalSummary, regularSummary));
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(professionalFlagQuery);
        when(professionalFlagQuery.setParameter(eq("professionalSource"), eq(ParticipantSource.PROFESSIONAL))).thenReturn(professionalFlagQuery);
        when(professionalFlagQuery.setParameter(eq("tournamentIds"), any())).thenReturn(professionalFlagQuery);
        when(professionalFlagQuery.getResultList()).thenReturn(List.of(
                new Object[]{professionalTournamentId, 3L, 3L},
                new Object[]{regularTournamentId, 2L, 1L}
        ));

        List<TournamentSummary> summaries = repositoryAdapter.findSummaries();

        assertEquals(2, summaries.size());
        assertEquals("Open Pro", summaries.get(0).name());
        assertTrue(summaries.get(0).professionalTournament());
        assertEquals("Open Social", summaries.get(1).name());
        assertFalse(summaries.get(1).professionalTournament());
        verifyNoInteractions(mapper, matchDomainMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_empty_summaries_when_no_tournaments_exist() {
        TypedQuery<TournamentSummary> summaryQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(TournamentSummary.class))).thenReturn(summaryQuery);
        when(summaryQuery.getResultList()).thenReturn(List.of());

        List<TournamentSummary> summaries = repositoryAdapter.findSummaries();

        assertTrue(summaries.isEmpty());
        verifyNoInteractions(mapper, matchDomainMapper);
    }

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
                .startTime(LocalTime.of(9, 0))
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
                .startTime(LocalTime.of(9, 0))
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

    @Test
    void should_create_new_tournament_when_not_found_in_database() {
        UUID tournamentId = UUID.randomUUID();
        TournamentEntity newEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("New Tournament")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of())
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.empty());
        when(tournamentJpaRepository.save(any(TournamentEntity.class))).thenReturn(newEntity);
        when(mapper.toEntity(domainTournament)).thenReturn(newEntity);
        when(mapper.toDomain(newEntity)).thenReturn(domainTournament);

        Tournament savedTournament = repositoryAdapter.save(domainTournament);

        verify(tournamentJpaRepository).save(newEntity);
        verify(mapper).toEntity(domainTournament);
        assertSame(domainTournament, savedTournament);
    }

    @Test
    void should_preserve_existing_draws_when_stage_payload_does_not_include_them() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        StageEntity stageEntity = StageEntity.builder()
                .id(stageId)
                .event(eventEntity)
                .order(1)
                .stageType("SINGLE_ELIMINATION")
                .description("Main draw")
                .draws(new ArrayList<>())
                .build();

        DrawEntity drawEntity = DrawEntity.builder()
                .id(drawId)
                .stage(stageEntity)
                .drawType(DrawType.ELIMINATION.name())
                .label("Draw 1")
                .build();

        stageEntity.getDraws().add(drawEntity);
        eventEntity.getStages().add(stageEntity);
        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(stageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .description("Main draw")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, stageEntity.getDraws().size());
        assertSame(drawEntity, stageEntity.getDraws().get(0));
    }

    @Test
    void should_add_new_stages_when_not_present_in_existing_entity() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID newStageId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(newStageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .description("New Stage")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, eventEntity.getStages().size());
        StageEntity addedStage = eventEntity.getStages().get(0);
        assertEquals(newStageId, addedStage.getId());
        assertEquals("New Stage", addedStage.getDescription());
    }

    @Test
    void should_remove_stages_not_in_incoming_event() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID keptStageId = UUID.randomUUID();
        UUID removedStageId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        StageEntity keptStage = StageEntity.builder()
                .id(keptStageId)
                .event(eventEntity)
                .order(1)
                .stageType("MAIN")
                .description("Kept")
                .draws(new ArrayList<>())
                .build();

        StageEntity removedStage = StageEntity.builder()
                .id(removedStageId)
                .event(eventEntity)
                .order(2)
                .stageType("CONSOLATION")
                .description("Removed")
                .draws(new ArrayList<>())
                .build();

        eventEntity.getStages().add(keptStage);
        eventEntity.getStages().add(removedStage);
        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(keptStageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .description("Kept")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, eventEntity.getStages().size());
        assertEquals(keptStageId, eventEntity.getStages().get(0).getId());
    }

    @Test
    void should_remove_events_not_in_incoming_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID keptEventId = UUID.randomUUID();
        UUID removedEventId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity keptEvent = EventEntity.builder()
                .id(keptEventId)
                .gender("MALE")
                .tournament(tournamentEntity)
                .build();

        EventEntity removedEvent = EventEntity.builder()
                .id(removedEventId)
                .gender("FEMALE")
                .tournament(tournamentEntity)
                .build();

        tournamentEntity.getEvents().add(keptEvent);
        tournamentEntity.getEvents().add(removedEvent);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder().id(keptEventId).categoryId(1).gender("MALE").build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, tournamentEntity.getEvents().size());
        assertEquals(keptEventId, tournamentEntity.getEvents().get(0).getId());
    }

    @Test
    void should_add_new_draws_to_existing_stage() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID newDrawId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        StageEntity stageEntity = StageEntity.builder()
                .id(stageId)
                .event(eventEntity)
                .order(1)
                .stageType("MAIN")
                .description("Main")
                .draws(new ArrayList<>())
                .build();

        eventEntity.getStages().add(stageEntity);
        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(stageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .description("Main")
                                                .draws(List.of(
                                                        Draw.builder()
                                                                .id(newDrawId)
                                                                .stageId(stageId)
                                                                .drawType(DrawType.ELIMINATION)
                                                                .drawName("New Draw")
                                                                .label("New Draw")
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, stageEntity.getDraws().size());
        assertEquals(newDrawId, stageEntity.getDraws().get(0).getId());
    }

    @Test
    void should_update_existing_draw_on_save() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        StageEntity stageEntity = StageEntity.builder()
                .id(stageId)
                .event(eventEntity)
                .order(1)
                .stageType("MAIN")
                .draws(new ArrayList<>())
                .build();

        DrawEntity existingDraw = DrawEntity.builder()
                .id(drawId)
                .stage(stageEntity)
                .drawType(DrawType.ELIMINATION.name())
                .label("Old Label")
                .build();

        stageEntity.getDraws().add(existingDraw);
        eventEntity.getStages().add(stageEntity);
        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(stageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .draws(List.of(
                                                        Draw.builder()
                                                                .id(drawId)
                                                                .stageId(stageId)
                                                                .drawType(DrawType.ELIMINATION)
                                                                .drawName("Updated Draw")
                                                                .label("Updated Label")
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, stageEntity.getDraws().size());
        assertEquals("Updated Label", stageEntity.getDraws().get(0).getLabel());
    }

    @Test
    void should_remove_draws_not_in_incoming_stage() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID keptDrawId = UUID.randomUUID();
        UUID removedDrawId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        StageEntity stageEntity = StageEntity.builder()
                .id(stageId)
                .event(eventEntity)
                .order(1)
                .stageType("MAIN")
                .draws(new ArrayList<>())
                .build();

        DrawEntity keptDraw = DrawEntity.builder()
                .id(keptDrawId)
                .stage(stageEntity)
                .drawType(DrawType.ELIMINATION.name())
                .label("Kept")
                .build();

        DrawEntity removedDraw = DrawEntity.builder()
                .id(removedDrawId)
                .stage(stageEntity)
                .drawType(DrawType.CONSOLATION.name())
                .label("Removed")
                .build();

        stageEntity.getDraws().add(keptDraw);
        stageEntity.getDraws().add(removedDraw);
        eventEntity.getStages().add(stageEntity);
        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(stageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .draws(List.of(
                                                        Draw.builder()
                                                                .id(keptDrawId)
                                                                .stageId(stageId)
                                                                .drawType(DrawType.ELIMINATION)
                                                                .drawName("Kept")
                                                                .label("Kept")
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertEquals(1, stageEntity.getDraws().size());
        assertEquals(keptDrawId, stageEntity.getDraws().get(0).getId());
    }

    @Test
    void should_skip_draw_update_when_domain_draws_are_null() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        TournamentEntity tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        EventEntity eventEntity = EventEntity.builder()
                .id(eventId)
                .gender("MALE")
                .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                .tournament(tournamentEntity)
                .stages(new ArrayList<>())
                .build();

        StageEntity stageEntity = StageEntity.builder()
                .id(stageId)
                .event(eventEntity)
                .order(1)
                .stageType("MAIN")
                .draws(new ArrayList<>())
                .build();

        eventEntity.getStages().add(stageEntity);
        tournamentEntity.getEvents().add(eventEntity);

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder()
                                .id(eventId)
                                .categoryId(1)
                                .gender("MALE")
                                .stages(List.of(
                                        Stage.builder()
                                                .id(stageId)
                                                .eventId(eventId)
                                                .stageNumber(1)
                                                .stageType(StageType.MAIN)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(tournamentJpaRepository.findById(tournamentId)).thenReturn(Optional.of(tournamentEntity));
        when(entityManager.getReference(RefAgeCategoryEntity.class, 1)).thenReturn(RefAgeCategoryEntity.builder().id(1).build());
        when(mapper.toDomain(tournamentEntity)).thenReturn(domainTournament);

        repositoryAdapter.save(domainTournament);

        assertTrue(stageEntity.getDraws().isEmpty());
    }

    @Test
    void should_find_all_tournaments() {
        UUID tournamentId = UUID.randomUUID();
        TournamentEntity entity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of())
                .build();

        when(tournamentJpaRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainTournament);

        List<Tournament> result = repositoryAdapter.findAll();

        assertEquals(1, result.size());
        assertEquals(tournamentId, result.get(0).getId());
        verify(mapper).toDomain(entity);
    }

    @Test
    void should_return_empty_list_when_find_all_returns_nothing() {
        when(tournamentJpaRepository.findAll()).thenReturn(List.of());

        List<Tournament> result = repositoryAdapter.findAll();

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_find_tournament_by_id() {
        UUID tournamentId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery);
        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId,
                "Open Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                40.4168,
                -3.7038,
                "placeId123",
                "Formatted Address",
                TournamentStatus.DRAFT,
                UUID.randomUUID()
        }));
        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        assertEquals(tournamentId, result.get().getId());
        assertEquals("Open Primavera", result.get().getName());
        assertEquals(Surface.CLAY, result.get().getSurface());
        assertEquals(32, result.get().getMaxPlayers());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_empty_when_tournament_not_found() {
        UUID tournamentId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(tournamentQuery);
        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_find_detailed_events_for_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> stageQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> drawQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery)
                .thenReturn(stageQuery)
                .thenReturn(drawQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId,
                "Open Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                null,
                null,
                null,
                null,
                TournamentStatus.DRAFT,
                UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                eventId,
                1,
                "MALE"
        }));

        UUID stageId = UUID.randomUUID();
        when(stageQuery.setParameter(eq("eventIds"), any())).thenReturn(stageQuery);
        when(stageQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                stageId,
                eventId,
                1,
                "MAIN",
                "Main Stage"
        }));

        when(drawQuery.setParameter(eq("stageIds"), any())).thenReturn(drawQuery);
        when(drawQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getEvents().size());
        Event event = result.get().getEvents().get(0);
        assertEquals(eventId, event.getId());
        assertEquals("MALE", event.getGender());
        assertEquals(1, event.getCategoryId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_find_detailed_draws_and_matches() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> stageQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> drawQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery)
                .thenReturn(stageQuery)
                .thenReturn(drawQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId,
                "Open Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                null,
                null,
                null,
                null,
                TournamentStatus.DRAFT,
                null
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                eventId,
                1,
                "MALE"
        }));

        when(stageQuery.setParameter(eq("eventIds"), any())).thenReturn(stageQuery);
        when(stageQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                stageId,
                eventId,
                1,
                "MAIN",
                "Main Stage"
        }));

        when(drawQuery.setParameter(eq("stageIds"), any())).thenReturn(drawQuery);
        when(drawQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                drawId,
                stageId,
                "ELIMINATION",
                "Main Draw"
        }));

        when(matchJpaRepository.findByTournamentId(tournamentId)).thenReturn(List.of());
        when(matchDomainMapper.toDomainList(List.of())).thenReturn(List.of());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        Event event = result.get().getEvents().get(0);
        Stage stage = event.getStages().get(0);
        Draw draw = stage.getDraws().get(0);
        assertEquals(drawId, draw.getId());
        assertEquals(DrawType.ELIMINATION, draw.getDrawType());
        assertEquals("Main Draw", draw.getDrawName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_find_tournament_with_null_created_by() {
        UUID tournamentId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery);
        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId,
                "Open Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                null,
                null,
                null,
                null,
                TournamentStatus.DRAFT,
                null
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        assertNull(result.get().getCreatedBy());
    }

    @Test
    void should_enrich_matches_on_find_all() {
        UUID tournamentId = UUID.randomUUID();
        TournamentEntity entity = TournamentEntity.builder()
                .id(tournamentId)
                .events(new ArrayList<>())
                .build();

        Tournament domainTournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of())
                .build();

        when(tournamentJpaRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainTournament);

        List<Tournament> result = repositoryAdapter.findAll();

        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_enrich_matches_with_null_tournament() {
        when(tournamentJpaRepository.findAll()).thenReturn(List.of());

        List<Tournament> result = repositoryAdapter.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_detailed_stages_with_null_stage_type() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> stageQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> drawQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery)
                .thenReturn(stageQuery)
                .thenReturn(drawQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId,
                "Open Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                null, null, null, null,
                TournamentStatus.DRAFT,
                UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                eventId, 1, "MALE"
        }));

        when(stageQuery.setParameter(eq("eventIds"), any())).thenReturn(stageQuery);
        when(stageQuery.getResultList()).thenAnswer(i -> java.util.List.<Object[]>of(new Object[]{
                stageId, eventId, 1, "MAIN", "Description"
        }));

        when(drawQuery.setParameter(eq("stageIds"), any())).thenReturn(drawQuery);
        when(drawQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        Stage stage = result.get().getEvents().get(0).getStages().get(0);
        assertEquals(StageType.MAIN, stage.getStageType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_detailed_draws_with_null_draw_type() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> stageQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> drawQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery)
                .thenReturn(stageQuery)
                .thenReturn(drawQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId, "Open Primavera",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                Surface.CLAY, 32, "Club Central",
                null, null, null, null,
                TournamentStatus.DRAFT, UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                eventId, 1, "MALE"
        }));

        when(stageQuery.setParameter(eq("eventIds"), any())).thenReturn(stageQuery);
        when(stageQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                stageId, eventId, 1, "MAIN", "Main"
        }));

        when(drawQuery.setParameter(eq("stageIds"), any())).thenReturn(drawQuery);
        when(drawQuery.getResultList()).thenAnswer(i -> java.util.List.<Object[]>of(new Object[]{
                drawId, stageId, "ELIMINATION", "Draw Label"
        }));

        when(matchJpaRepository.findByTournamentId(tournamentId)).thenReturn(java.util.List.of());
        when(matchDomainMapper.toDomainList(java.util.List.of())).thenReturn(java.util.List.of());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        Draw draw = result.get().getEvents().get(0).getStages().get(0).getDraws().get(0);
        assertEquals(DrawType.ELIMINATION, draw.getDrawType());
        assertEquals("Draw Label", draw.getDrawName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_empty_events_list_in_detailed_query() {
        UUID tournamentId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId, "Open Primavera",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                Surface.CLAY, 32, "Club Central",
                null, null, null, null,
                TournamentStatus.DRAFT, UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        assertTrue(result.get().getEvents().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_stages_with_empty_draws_in_detailed_query() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> stageQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> drawQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery)
                .thenReturn(stageQuery)
                .thenReturn(drawQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId, "Open Primavera",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                Surface.CLAY, 32, "Club Central",
                null, null, null, null,
                TournamentStatus.DRAFT, UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                eventId, 1, "MALE"
        }));

        when(stageQuery.setParameter(eq("eventIds"), any())).thenReturn(stageQuery);
        when(stageQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                stageId, eventId, 1, "MAIN", "Main"
        }));

        when(drawQuery.setParameter(eq("stageIds"), any())).thenReturn(drawQuery);
        when(drawQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
        Stage stage = result.get().getEvents().get(0).getStages().get(0);
        assertTrue(stage.getDraws().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_stages_with_null_draws_in_enrich() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> stageQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> drawQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery)
                .thenReturn(stageQuery)
                .thenReturn(drawQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId, "Open Primavera",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                Surface.CLAY, 32, "Club Central",
                null, null, null, null,
                TournamentStatus.DRAFT, UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                eventId, 1, "MALE"
        }));

        when(stageQuery.setParameter(eq("eventIds"), any())).thenReturn(stageQuery);
        when(stageQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                stageId, eventId, 1, "MAIN", "Main"
        }));

        when(drawQuery.setParameter(eq("stageIds"), any())).thenReturn(drawQuery);
        when(drawQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handle_enrich_matches_with_null_events_on_domain() {
        UUID tournamentId = UUID.randomUUID();

        TypedQuery<Object[]> tournamentQuery = mock(TypedQuery.class);
        TypedQuery<Object[]> eventQuery = mock(TypedQuery.class);

        when(entityManager.createQuery(anyString(), eq(Object[].class)))
                .thenReturn(tournamentQuery)
                .thenReturn(eventQuery);

        when(tournamentQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(tournamentQuery);
        when(tournamentQuery.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                tournamentId, "Open Primavera",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                Surface.CLAY, 32, "Club Central",
                null, null, null, null,
                TournamentStatus.DRAFT, UUID.randomUUID()
        }));

        when(eventQuery.setParameter(eq("tournamentId"), eq(tournamentId))).thenReturn(eventQuery);
        when(eventQuery.getResultList()).thenAnswer(i -> emptyObjectArrayResult());

        Optional<Tournament> result = repositoryAdapter.findById(tournamentId);

        assertTrue(result.isPresent());
    }

    private static void assertNull(Object value) {
        org.junit.jupiter.api.Assertions.assertNull(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> emptyObjectArrayResult() {
        return List.of();
    }
}
