package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AgeCategoryRefMapper {
    AgeCategoryRef toDomain(RefAgeCategoryEntity entity);

    RefAgeCategoryEntity toEntity(AgeCategoryRef domain);

    void updateEntity(AgeCategoryRef domain, @MappingTarget RefAgeCategoryEntity entity);
}
