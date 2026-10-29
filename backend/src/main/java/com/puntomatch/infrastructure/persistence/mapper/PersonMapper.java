package com.puntomatch.infrastructure.persistence.mapper;

import com.puntomatch.domain.models.Person;
import com.puntomatch.infrastructure.persistence.entity.PersonEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    Person toDomain(PersonEntity entity);

    PersonEntity toEntity(Person domain);
}
