package com.puntomatch.infrastructure.controller.mapper;

import com.puntomatch.domain.models.Member;
import com.puntomatch.infrastructure.controller.dto.MemberRequest;
import com.puntomatch.infrastructure.controller.dto.MemberResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberWebMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    Member toDomain(MemberRequest request);

    MemberResponse toResponse(Member domain);
}
