package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.MatchScore;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.SetScoreResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchWebMapper {

    @Mapping(target = "firstInscriptionId", source = "firstInscription.id")
    @Mapping(target = "secondInscriptionId", source = "secondInscription.id")
    @Mapping(target = "winnerId", source = "winner.id")
    @Mapping(target = "scheduleTimeType", source = "scheduleTimeType")
    @Mapping(target = "status", expression = "java(domain.getStatus() != null ? domain.getStatus().name() : null)")
    @Mapping(target = "sets", expression = "java(mapScoreToSetsResponse(domain))")
    @Mapping(target = "notes", source = "notes")
    MatchResponse toResponse(Match domain);

    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "firstInscription.id", source = "firstInscriptionId")
    @Mapping(target = "secondInscription.id", source = "secondInscriptionId")
    @Mapping(target = "winner.id", source = "winnerId")
    @Mapping(target = "nextMatch", ignore = true)
    @Mapping(target = "loserNextMatch", ignore = true)
    @Mapping(target = "score", expression = "java(mapSetsResponseToScore(request.sets()))")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "status", expression = "java(request.status() != null ? com.tfm.tennis_platform.domain.models.enums.MatchStatus.valueOf(request.status()) : null)")
    Match toDomain(MatchResponse request);

    default java.util.List<SetScoreResponse> mapScoreToSetsResponse(Match domain) {
        if (domain == null || domain.getScore() == null || domain.getScore().getSets() == null) {
            return java.util.Collections.emptyList();
        }
        return domain.getScore().getSets().stream()
                .map(set -> new SetScoreResponse(
                        set.getSetNumber(),
                        set.getFirstPlayerGames(),
                        set.getSecondPlayerGames(),
                        set.getFirstPlayerTiebreak(),
                        set.getSecondPlayerTiebreak()
                ))
                .toList();
    }

    default MatchScore mapSetsResponseToScore(java.util.List<SetScoreResponse> sets) {
        if (sets == null) {
            return MatchScore.empty();
        }
        java.util.List<com.tfm.tennis_platform.domain.models.SetScore> domainSets = sets.stream()
                .map(set -> com.tfm.tennis_platform.domain.models.SetScore.builder()
                        .setNumber(set.setNumber())
                        .firstPlayerGames(set.firstPlayerGames())
                        .secondPlayerGames(set.secondPlayerGames())
                        .firstPlayerTiebreak(set.firstPlayerTiebreak())
                        .secondPlayerTiebreak(set.secondPlayerTiebreak())
                        .build())
                .toList();
        return MatchScore.builder().sets(domainSets).build();
    }
}
