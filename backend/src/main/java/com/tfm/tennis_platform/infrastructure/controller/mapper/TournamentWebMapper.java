package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.DrawResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.StageResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentEventResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TournamentWebMapper {
    TournamentWebMapper INSTANCE = Mappers.getMapper(TournamentWebMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "name", source = "formalName")
    @Mapping(target = "playPeriod", expression = "java(toPeriod(request.playStartDate(), request.playEndDate()))")
    @Mapping(target = "startTime", source = "tournamentStartTime")
    @Mapping(target = "inscriptionPeriod", expression = "java(toPeriod(request.inscriptionStartDate(), request.inscriptionEndDate()))")
    @Mapping(target = "surface", source = "surfaceCategory")
    Tournament toDomain(TournamentRequest request);

    @Mapping(target = "formalName", source = "name")
    @Mapping(target = "playStartDate", source = "playPeriod.startDate")
    @Mapping(target = "playEndDate", source = "playPeriod.endDate")
    @Mapping(target = "tournamentStartTime", source = "startTime")
    @Mapping(target = "inscriptionStartDate", source = "inscriptionPeriod.startDate")
    @Mapping(target = "inscriptionEndDate", source = "inscriptionPeriod.endDate")
    @Mapping(target = "surfaceCategory", source = "surface")
    @Mapping(target = "status", source = "state")
    @Mapping(target = "providerOrganisationId", source = "createdBy")
    @Mapping(target = "events", expression = "java(toEventResponses(domain.getEvents()))")
    @Mapping(target = "professionalTournament", expression = "java(Boolean.FALSE)")
    TournamentResponse toResponse(Tournament domain);

    @Mapping(target = "events", ignore = true)
    Tournament toDomain(TournamentEntity entity);

    @Mapping(target = "events", ignore = true)
    TournamentEntity toEntity(Tournament domain);

    default Tournament toDomain(TournamentEntity entity, List<EventEntity> eventEntities) {
        Tournament t = toDomain(entity);
        List<Event> events = eventEntities != null ? eventEntities.stream().map(this::toDomain).toList() : List.of();
        return Tournament.builder()
                .id(t.getId())
                .name(t.getName())
                .playPeriod(t.getPlayPeriod())
                .startTime(t.getStartTime())
                .inscriptionPeriod(t.getInscriptionPeriod())
                .surface(t.getSurface())
                .maxPlayers(t.getMaxPlayers())
                .location(t.getLocation())
                .locationLatitude(t.getLocationLatitude())
                .locationLongitude(t.getLocationLongitude())
                .locationPlaceId(t.getLocationPlaceId())
                .locationFormattedAddress(t.getLocationFormattedAddress())
                .state(t.getState())
                .createdBy(t.getCreatedBy())
                .events(events)
                .build();
    }

    default Event toDomain(EventEntity entity) {
        return Event.builder()
                .id(entity.getId())
                .tournamentId(entity.getTournament() != null ? entity.getTournament().getId() : null)
                .categoryId(entity.getAgeCategory() != null ? entity.getAgeCategory().getId() : null)
                .gender(entity.getGender())
                .build();
    }

    default EventEntity toEntity(Event domain) {
        return EventEntity.builder()
                .ageCategory(mapRefAgeCategory(domain.getCategoryId()))
                .gender(domain.getGender())
                .build();
    }

    default List<TournamentEventResponse> toEventResponses(List<Event> events) {
        if (events == null) {
            return List.of();
        }

        return events.stream()
            .map(event -> new TournamentEventResponse(
                    event.getId(),
                    event.getCategoryId(),
                    event.getGender(),
                    toStageResponses(event.getStages())
            ))
                .toList();
    }

    default List<StageResponse> toStageResponses(List<Stage> stages) {
        if (stages == null) {
            return List.of();
        }

        return stages.stream()
            .map(stage -> new StageResponse(
                stage.getId(),
                stage.getEventId(),
                stage.getStageType(),
                stage.getStageNumber(),
                stage.getStrategyName(),
                stage.getDescription(),
                toDrawResponses(stage.getDraws())
            ))
                .toList();
    }

    default List<DrawResponse> toDrawResponses(List<Draw> draws) {
        if (draws == null) {
            return List.of();
        }

        return draws.stream()
                .map(draw -> new DrawResponse(
                    draw.getId(),
                    draw.getStageId(),
                    draw.getDrawType(),
                    draw.getLabel(),
                    draw.getGroupIndex(),
                    toMatchResponses(draw.getMatches())
                ))
                .toList();
    }

    default List<MatchResponse> toMatchResponses(List<Match> matches) {
        if (matches == null) {
            return List.of();
        }

        return matches.stream()
                .map(match -> new MatchResponse(
                    match.getId(),
                    match.getFirstInscriptionId(),
                    match.getSecondInscriptionId(),
                    match.getWinnerId(),
                    match.getRoundNumber(),
                    match.getBracketPosition(),
                    match.getScheduledAt(),
                    match.getScheduleTimeType() != null ? match.getScheduleTimeType().name() : null,
                    match.getCourtId(),
                    match.getCourt(),
                    match.getResult(),
                    match.isProfessionalMatch(),
                    match.getFirstWinPoints(),
                    match.getSecondWinPoints(),
                    match.getStatus() != null ? match.getStatus().name() : null
                ))
                .toList();
    }

    default RefAgeCategoryEntity mapRefAgeCategory(Integer ageCategoryId) {
        if (ageCategoryId == null) {
            return null;
        }

        return RefAgeCategoryEntity.builder().id(ageCategoryId).build();
    }

    default TournamentPeriod toPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return new TournamentPeriod(startDate, endDate);
    }
}
