package com.puntomatch.infrastructure.persistence.mapper;

import com.puntomatch.domain.models.AgeCategoryRef;
import com.puntomatch.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgeCategoryRefMapper {
    AgeCategoryRef toDomain(RefAgeCategoryEntity entity);
}
