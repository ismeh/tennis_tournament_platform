package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TournamentWebMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "eventIds", ignore = true)
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

    default TournamentPeriod toPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        return new TournamentPeriod(startDate, endDate);
    }
}
