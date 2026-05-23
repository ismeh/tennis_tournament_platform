package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TournamentRepositoryAdapter implements TournamentRepository {

    private final JpaTournamentRepository tournamentJpaRepository;
    private final TournamentEntityMapper mapper;
    private final EntityManager entityManager;

    @Override
    public Tournament save(Tournament tournament) {
        TournamentEntity tournamentEntity = tournamentJpaRepository.findById(tournament.getId())
                .orElseGet(() -> tournamentJpaRepository.save(mapper.toEntity(tournament)));
        mapper.updateEntityFromDomain(tournament, tournamentEntity);

        Map<UUID, EventEntity> existingEventsById = tournamentEntity.getEvents().stream()
            .collect(Collectors.toMap(EventEntity::getId, event -> event, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> incomingEventIds = tournament.getEvents().stream()
            .map(domainEvent -> domainEvent.getId())
            .collect(Collectors.toSet());

        tournamentEntity.getEvents().removeIf(eventEntity -> !incomingEventIds.contains(eventEntity.getId()));

        for (var domainEvent : tournament.getEvents()) {
            RefAgeCategoryEntity categoryProxy = entityManager.getReference(
                RefAgeCategoryEntity.class,
                domainEvent.getCategoryId()
            );

            EventEntity existingEvent = existingEventsById.get(domainEvent.getId());

            if (existingEvent != null) {
                existingEvent.setGender(domainEvent.getGender());
                existingEvent.setAgeCategory(categoryProxy);
                existingEvent.setTournament(tournamentEntity);
                updateEventStages(existingEvent, domainEvent);
                continue;
            }

            EventEntity newEvent = EventEntity.builder()
                .id(domainEvent.getId())
                .gender(domainEvent.getGender())
                .ageCategory(categoryProxy)
                .tournament(tournamentEntity)
                .build();
            updateEventStages(newEvent, domainEvent);
            tournamentEntity.getEvents().add(newEvent);
        }

        tournamentJpaRepository.flush();

        return mapper.toDomain(tournamentEntity);
    }

    @Override
    public List<Tournament> findAll() {
        return tournamentJpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Tournament> findById(UUID id) {
        return tournamentJpaRepository.findById(id).map(mapper::toDomain);
    }

    private void updateEventStages(EventEntity eventEntity, Event domainEvent) {
        Map<UUID, StageEntity> existingStagesById = eventEntity.getStages().stream()
            .collect(Collectors.toMap(StageEntity::getId, stage -> stage, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> incomingStageIds = domainEvent.getStages().stream()
            .map(domainStage -> domainStage.getId())
            .collect(Collectors.toSet());

        eventEntity.getStages().removeIf(stageEntity -> !incomingStageIds.contains(stageEntity.getId()));

        for (var domainStage : domainEvent.getStages()) {
            StageEntity existingStage = existingStagesById.get(domainStage.getId());

            if (existingStage != null) {
                existingStage.setOrder(domainStage.getStageNumber());
                existingStage.setStageType(domainStage.getStageType() != null ? domainStage.getStageType().name() : null);
                existingStage.setDescription(domainStage.getDescription());
                updateStageDraws(existingStage, domainStage);
                continue;
            }

            StageEntity newStage = StageEntity.builder()
                .id(domainStage.getId())
                .event(eventEntity)
                .order(domainStage.getStageNumber())
                .stageType(domainStage.getStageType() != null ? domainStage.getStageType().name() : null)
                .description(domainStage.getDescription())
                .build();
            updateStageDraws(newStage, domainStage);
            eventEntity.getStages().add(newStage);
        }
    }

    private void updateStageDraws(StageEntity stageEntity, Stage domainStage) {
        if (domainStage.getDraws() == null || domainStage.getDraws().isEmpty()) {
            return;
        }

        Map<UUID, DrawEntity> existingDrawsById = stageEntity.getDraws().stream()
            .collect(Collectors.toMap(DrawEntity::getId, draw -> draw, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> incomingDrawIds = domainStage.getDraws().stream()
            .map(Draw::getId)
            .collect(Collectors.toSet());

        stageEntity.getDraws().removeIf(drawEntity -> !incomingDrawIds.contains(drawEntity.getId()));

        for (var domainDraw : domainStage.getDraws()) {
            DrawEntity existingDraw = existingDrawsById.get(domainDraw.getId());

            if (existingDraw != null) {
                existingDraw.setDrawType(domainDraw.getDrawType() != null ? domainDraw.getDrawType().name() : null);
                existingDraw.setLabel(domainDraw.getLabel() != null ? domainDraw.getLabel() : domainDraw.getDrawName());
                existingDraw.setStage(stageEntity);
                continue;
            }

            DrawEntity newDraw = DrawEntity.builder()
                .id(domainDraw.getId())
                .stage(stageEntity)
                .drawType(domainDraw.getDrawType() != null ? domainDraw.getDrawType().name() : null)
                .label(domainDraw.getLabel() != null ? domainDraw.getLabel() : domainDraw.getDrawName())
                .build();
            stageEntity.getDraws().add(newDraw);
        }
    }
}
