package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapping;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {MemberMapper.class})
public interface TournamentEntityMapper {
    @Mapping(target = "name", source = "formalName")
    @Mapping(target = "playPeriod", expression = "java(toPeriod(entity.getPlayStartDate(), entity.getPlayEndDate()))")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "inscriptionPeriod", expression = "java(toPeriod(entity.getInscriptionStartDate(), entity.getInscriptionEndDate()))")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "state", source = "status")
    @Mapping(target = "events", expression = "java(toDomainEvents(entity.getEvents()))")
    Tournament toDomain(TournamentEntity entity);

    @Mapping(target = "formalName", source = "name")
    @Mapping(target = "playStartDate", source = "playPeriod.startDate")
    @Mapping(target = "playEndDate", source = "playPeriod.endDate")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "inscriptionStartDate", source = "inscriptionPeriod.startDate")
    @Mapping(target = "inscriptionEndDate", source = "inscriptionPeriod.endDate")
    @Mapping(target = "status", source = "state")
    @Mapping(target = "events", expression = "java(toEntityEvents(domain.getEvents()))")
    TournamentEntity toEntity(Tournament domain);

    default TournamentPeriod toPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        return new TournamentPeriod(startDate, endDate);
    }

    default MemberEntity map(UUID createdBy) {
        if (createdBy == null) {
            return null;
        }

        return MemberEntity.builder().id(createdBy).build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target= "status", source = "state")
    @Mapping(target = "startTime", source = "startTime")
    void updateEntityFromDomain(Tournament domain, @MappingTarget TournamentEntity entity);

    default List<Event> toDomainEvents(List<EventEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default List<EventEntity> toEntityEvents(List<Event> events) {
        if (events == null) return new ArrayList<>();
        return events.stream()
                .map(this::toEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default Event toDomain(EventEntity entity) {
        if (entity == null) return null;
        return Event.builder()
                .id(entity.getId())
                .categoryId(entity.getAgeCategory() != null ? entity.getAgeCategory().getId() : null)
                .gender(entity.getGender())
                .stages(toDomainStages(entity.getStages()))
                .build();
    }

    default EventEntity toEntity(Event event) {
        if (event == null) return null;
        return EventEntity.builder()
                .id(event.getId())
                .ageCategory(mapRefAgeCategory(event.getCategoryId()))
                .gender(event.getGender())
                .stages(toEntityStages(event.getStages()))
                .build();
    }

    default List<Stage> toDomainStages(List<StageEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default List<StageEntity> toEntityStages(List<Stage> stages) {
        if (stages == null) return new ArrayList<>();
        return stages.stream()
                .map(this::toEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default Stage toDomain(StageEntity entity) {
        if (entity == null) return null;
        return Stage.builder()
                .id(entity.getId())
                .eventId(entity.getEvent() != null ? entity.getEvent().getId() : null)
                .stageNumber(entity.getOrder())
                .stageType(entity.getStageType() != null ? StageType.valueOf(entity.getStageType()) : null)
                .strategyName(entity.getStrategyName())
                .description(entity.getDescription())
                .draws(toDomainDraws(entity.getDraws()))
                .build();
    }

    default StageEntity toEntity(Stage stage) {
        if (stage == null) return null;
        return StageEntity.builder()
                .id(stage.getId())
                .order(stage.getStageNumber())
                .stageType(stage.getStageType() != null ? stage.getStageType().name() : null)
                .strategyName(stage.getStrategyName())
                .description(stage.getDescription())
                .draws(toEntityDraws(stage.getDraws()))
                .build();
    }

    default List<Draw> toDomainDraws(List<DrawEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default List<DrawEntity> toEntityDraws(List<Draw> draws) {
        if (draws == null) return new ArrayList<>();
        return draws.stream()
                .map(this::toEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default Draw toDomain(DrawEntity entity) {
        if (entity == null) return null;
        return Draw.builder()
                .id(entity.getId())
                .stageId(entity.getStage() != null ? entity.getStage().getId() : null)
                .drawType(entity.getDrawType() != null ? DrawType.valueOf(entity.getDrawType()) : null)
                .drawName(entity.getLabel())
                .label(entity.getLabel())
                .groupIndex(entity.getGroupIndex())
                .matches(toDomainMatches(entity.getMatches()))
                .build();
    }

    default DrawEntity toEntity(Draw draw) {
        if (draw == null) return null;
        return DrawEntity.builder()
                .id(draw.getId())
                .drawType(draw.getDrawType() != null ? draw.getDrawType().name() : null)
                .label(draw.getLabel() != null ? draw.getLabel() : draw.getDrawName())
                .build();
    }

    default List<Match> toDomainMatches(List<MatchEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    default Match toDomain(MatchEntity entity) {
        if (entity == null) return null;
        return Match.builder()
                .id(entity.getId())
                .drawId(entity.getDraw() != null ? entity.getDraw().getId() : null)
                .firstInscription(mapInscriptionDomain(entity.getFirstInscription() != null ? entity.getFirstInscription().getId() : null))
                .secondInscription(mapInscriptionDomain(entity.getSecondInscription() != null ? entity.getSecondInscription().getId() : null))
                .winner(mapInscriptionDomain(entity.getWinner() != null ? entity.getWinner().getId() : null))
                .roundNumber(entity.getRoundNumber())
                .bracketPosition(entity.getBracketPosition())
                .nextMatch(toDomain(entity.getNextMatch()))
                .loserNextMatch(toDomain(entity.getLoserNextMatch()))
                .scheduledAt(entity.getScheduledAt())
                .scheduleTimeType(entity.getScheduleTimeType())
                .courtId(entity.getCourtResource() != null ? entity.getCourtResource().getId() : null)
                .court(entity.getCourtResource() != null ? entity.getCourtResource().getName() : entity.getCourt())
                .result(entity.getResult())
                .build();
    }

    default Inscription mapInscriptionDomain(java.util.UUID inscriptionId) {
        if (inscriptionId == null) {
            return null;
        }

        return Inscription.builder()
                .id(inscriptionId)
                .eventId(null)
                .participantId(null)
                .status(null)
                .paymentStatus(null)
                .registeredAt(null)
                .build();
    }

    @AfterMapping
    default void linkEventsToTournament(@MappingTarget TournamentEntity tournamentEntity) {
        if (tournamentEntity.getEvents() == null) {
            return;
        }
        tournamentEntity.getEvents().forEach(event -> {
            event.setTournament(tournamentEntity);
            if (event.getStages() != null) {
                event.getStages().forEach(stage -> {
                    stage.setEvent(event);
                    if (stage.getDraws() != null) {
                        stage.getDraws().forEach(draw -> draw.setStage(stage));
                    }
                });
            }
        });
    }


    default RefAgeCategoryEntity mapRefAgeCategory(Integer ageCategoryId) {
        if (ageCategoryId == null) {
            return null;
        }

        return RefAgeCategoryEntity.builder().id(ageCategoryId).build();
    }
}
