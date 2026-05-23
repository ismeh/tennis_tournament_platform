package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgeCategoryRefMapper {
    AgeCategoryRef toDomain(RefAgeCategoryEntity entity);
}
