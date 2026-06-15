package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "password", source = "passwordHash")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "emailConfirmationTokenHash", source = "emailConfirmationTokenHash")
    @Mapping(target = "emailConfirmationExpiresAt", source = "emailConfirmationExpiresAt")
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "privacyPolicyAccepted", source = "privacyPolicyAccepted")
    @Mapping(target = "privacyPolicyAcceptedAt", source = "privacyPolicyAcceptedAt")
    @Mapping(target = "privacyPolicyVersion", source = "privacyPolicyVersion")
    Member toDomain(MemberEntity entity);

    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "emailConfirmationTokenHash", source = "emailConfirmationTokenHash")
    @Mapping(target = "emailConfirmationExpiresAt", source = "emailConfirmationExpiresAt")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "privacyPolicyAccepted", source = "privacyPolicyAccepted")
    @Mapping(target = "privacyPolicyAcceptedAt", source = "privacyPolicyAcceptedAt")
    @Mapping(target = "privacyPolicyVersion", source = "privacyPolicyVersion")
    MemberEntity toEntity(Member domain);
}
