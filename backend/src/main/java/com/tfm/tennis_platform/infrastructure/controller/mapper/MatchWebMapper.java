package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchWebMapper {

    @Mapping(target = "tournamentId", source = "tournament.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "firstInscriptionId", source = "firstInscription.id")
    @Mapping(target = "secondInscriptionId", source = "secondInscription.id")
    @Mapping(target = "winnerId", source = "winner.id")
    MatchResponse toResponse(Match domain);

    @Mapping(target = "tournament.id", source = "tournamentId")
    @Mapping(target = "category.id", source = "categoryId")
    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "firstInscription.id", source = "firstInscriptionId")
    @Mapping(target = "secondInscription.id", source = "secondInscriptionId")
    @Mapping(target = "winner.id", source = "winnerId")
    @Mapping(target = "nextMatch", ignore = true)
    Match toDomain(MatchResponse request);
}
