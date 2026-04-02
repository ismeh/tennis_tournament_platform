package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

import java.time.LocalDate;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = {MemberMapper.class, CategoryMapper.class})
public interface TournamentMapper {
    @Mapping(target = "name", source = "formalName")
    @Mapping(target = "playPeriod", expression = "java(toPeriod(entity.getPlayStartDate(), entity.getPlayEndDate()))")
    @Mapping(target = "inscriptionPeriod", expression = "java(toPeriod(entity.getInscriptionStartDate(), entity.getInscriptionEndDate()))")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "eventIds", ignore = true)
    @Mapping(target = "state", source = "status")
    Tournament toDomain(TournamentEntity entity);

    @Mapping(target = "formalName", source = "name")
    @Mapping(target = "playStartDate", source = "playPeriod.startDate")
    @Mapping(target = "playEndDate", source = "playPeriod.endDate")
    @Mapping(target = "inscriptionStartDate", source = "inscriptionPeriod.startDate")
    @Mapping(target = "inscriptionEndDate", source = "inscriptionPeriod.endDate")
    @Mapping(target = "status", source = "state")
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "events", ignore = true)
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
}
