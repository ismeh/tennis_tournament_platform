package com.puntomatch.infrastructure.persistence.mapper;

import com.puntomatch.domain.models.Role;
import com.puntomatch.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    Role toDomain(RoleEntity entity);
    RoleEntity toEntity(Role domain);
}
