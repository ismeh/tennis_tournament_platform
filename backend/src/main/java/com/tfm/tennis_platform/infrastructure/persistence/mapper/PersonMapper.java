package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    @Mapping(source = "club.id", target = "clubId")
    @Mapping(source = "club.name", target = "clubName")
    Person toDomain(PersonEntity entity);

    @Mapping(target = "club", ignore = true)
    PersonEntity toEntity(Person domain);

    @AfterMapping
    default void resolveClub(@MappingTarget PersonEntity entity, Person domain) {
        if (domain.getClubId() != null) {
            ClubEntity club = new ClubEntity();
            club.setId(domain.getClubId());
            if (domain.getClubName() != null) {
                club.setName(domain.getClubName());
            }
            entity.setClub(club);
        } else {
            entity.setClub(null);
        }
    }
}
