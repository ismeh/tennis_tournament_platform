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
    Member toDomain(MemberRequest request);

    MemberResponse toResponse(Member domain);
}
