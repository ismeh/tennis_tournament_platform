package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "password", source = "passwordHash")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "personId", source = "personId")
    Member toDomain(MemberEntity entity);

    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "personId", source = "personId")
    MemberEntity toEntity(Member domain);
}
