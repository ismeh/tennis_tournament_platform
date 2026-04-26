package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    Person toDomain(PersonEntity entity);

    PersonEntity toEntity(Person domain);
}
