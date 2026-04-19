package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TournamentEntityMapper {
    TournamentEntityMapper INSTANCE = Mappers.getMapper(TournamentEntityMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "name", source = "formalName")
    @Mapping(target = "playPeriod", expression = "java(toPeriod(request.playStartDate(), request.playEndDate()))")
    @Mapping(target = "inscriptionPeriod", expression = "java(toPeriod(request.inscriptionStartDate(), request.inscriptionEndDate()))")
    @Mapping(target = "surface", source = "surfaceCategory")
    Tournament toDomain(TournamentRequest request);

    @Mapping(target = "formalName", source = "name")
    @Mapping(target = "playStartDate", source = "playPeriod.startDate")
    @Mapping(target = "playEndDate", source = "playPeriod.endDate")
    @Mapping(target = "inscriptionStartDate", source = "inscriptionPeriod.startDate")
    @Mapping(target = "inscriptionEndDate", source = "inscriptionPeriod.endDate")
    @Mapping(target = "surfaceCategory", source = "surface")
    @Mapping(target = "status", source = "state")
    @Mapping(target = "providerOrganisationId", source = "createdBy")
    TournamentResponse toResponse(Tournament domain);

    @Mapping(target = "events", ignore = true)
    Tournament toDomain(TournamentEntity entity);

    @Mapping(target = "events", ignore = true)
    TournamentEntity toEntity(Tournament domain);

    default Tournament toDomain(TournamentEntity entity, List<EventEntity> eventEntities) {
        Tournament t = toDomain(entity);
        List<Event> events = eventEntities != null ? eventEntities.stream().map(this::toDomain).collect(Collectors.toList()) : List.of();
        return Tournament.builder()
                .id(t.getId())
                .name(t.getName())
                .playPeriod(t.getPlayPeriod())
                .inscriptionPeriod(t.getInscriptionPeriod())
                .surface(t.getSurface())
                .maxPlayers(t.getMaxPlayers())
                .location(t.getLocation())
                .state(t.getState())
                .createdBy(t.getCreatedBy())
                .events(events)
                .build();
    }

    // Métodos para eventos (pueden ser delegados a otro mapper si existe)
    default Event toDomain(EventEntity entity) {
        return Event.builder()
                .categoryId(entity.getId())
                .gender(entity.getGender())
                .build();
    }

    default EventEntity toEntity(Event domain) {
        return EventEntity.builder()
                .id(domain.getCategoryId())
                .gender(domain.getGender())
                .build();
    }

    default TournamentPeriod toPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return new TournamentPeriod(startDate, endDate);
    }
}
