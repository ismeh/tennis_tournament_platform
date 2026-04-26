package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {MemberMapper.class, CategoryMapper.class})
public interface TournamentEntityMapper {
    @Mapping(target = "name", source = "formalName")
    @Mapping(target = "playPeriod", expression = "java(toPeriod(entity.getPlayStartDate(), entity.getPlayEndDate()))")
    @Mapping(target = "inscriptionPeriod", expression = "java(toPeriod(entity.getInscriptionStartDate(), entity.getInscriptionEndDate()))")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "state", source = "status")
    @Mapping(target = "events", expression = "java(toDomainEvents(entity.getEvents()))")
    Tournament toDomain(TournamentEntity entity);

    @Mapping(target = "formalName", source = "name")
    @Mapping(target = "playStartDate", source = "playPeriod.startDate")
    @Mapping(target = "playEndDate", source = "playPeriod.endDate")
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
                .build();
    }

    default EventEntity toEntity(Event event) {
        if (event == null) return null;
        return EventEntity.builder()
                .id(event.getId())
                .ageCategory(mapRefAgeCategory(event.getCategoryId()))
                .gender(event.getGender())
                .build();
    }

    @AfterMapping
    default void linkEventsToTournament(@MappingTarget TournamentEntity tournamentEntity) {
        if (tournamentEntity.getEvents() == null) {
            return;
        }
        tournamentEntity.getEvents().forEach(event -> event.setTournament(tournamentEntity));
    }


    default RefAgeCategoryEntity mapRefAgeCategory(Integer ageCategoryId) {
        if (ageCategoryId == null) {
            return null;
        }

        return RefAgeCategoryEntity.builder().id(ageCategoryId).build();
    }
}
