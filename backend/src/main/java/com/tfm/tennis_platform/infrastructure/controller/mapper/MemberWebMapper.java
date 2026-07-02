package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MemberResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberWebMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "tokenHash", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "emailConfirmationTokenHash", ignore = true)
    @Mapping(target = "emailConfirmationExpiresAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "privacyPolicyAccepted", ignore = true)
    @Mapping(target = "privacyPolicyAcceptedAt", ignore = true)
    @Mapping(target = "privacyPolicyVersion", ignore = true)
    @Mapping(target = "termsConditionsAccepted", ignore = true)
    @Mapping(target = "termsConditionsAcceptedAt", ignore = true)
    @Mapping(target = "termsConditionsVersion", ignore = true)
    Member toDomain(MemberRequest request);

    MemberResponse toResponse(Member domain);
}
