package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TournamentEntityMapper.class, CategoryMapper.class, InscriptionMapper.class})
public interface MatchMapper {
    Match toDomain(MatchEntity entity);
    MatchEntity toEntity(Match domain);
}
