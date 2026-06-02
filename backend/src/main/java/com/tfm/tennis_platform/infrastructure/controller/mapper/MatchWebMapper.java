package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchWebMapper {

    @Mapping(target = "firstInscriptionId", source = "firstInscription.id")
    @Mapping(target = "secondInscriptionId", source = "secondInscription.id")
    @Mapping(target = "winnerId", source = "winner.id")
    @Mapping(target = "scheduleTimeType", source = "scheduleTimeType")
    MatchResponse toResponse(Match domain);

    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "firstInscription.id", source = "firstInscriptionId")
    @Mapping(target = "secondInscription.id", source = "secondInscriptionId")
    @Mapping(target = "winner.id", source = "winnerId")
    @Mapping(target = "nextMatch", ignore = true)
    @Mapping(target = "loserNextMatch", ignore = true)
    Match toDomain(MatchResponse request);
}
