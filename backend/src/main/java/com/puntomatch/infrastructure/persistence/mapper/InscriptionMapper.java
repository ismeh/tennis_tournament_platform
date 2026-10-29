package com.puntomatch.infrastructure.persistence.mapper;

import com.puntomatch.domain.models.Inscription;
import com.puntomatch.infrastructure.persistence.entity.InscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InscriptionMapper {
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "participantId", source = "participant.id")
    Inscription toDomain(InscriptionEntity entity);

    @Mapping(target = "event.id", source = "eventId")
    @Mapping(target = "participant.id", source = "participantId")
    InscriptionEntity toEntity(Inscription domain);
}
