package com.puntomatch.infrastructure.persistence.mapper;

import com.puntomatch.domain.models.Category;
import com.puntomatch.infrastructure.persistence.entity.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toDomain(CategoryEntity entity);
    CategoryEntity toEntity(Category domain);
}
