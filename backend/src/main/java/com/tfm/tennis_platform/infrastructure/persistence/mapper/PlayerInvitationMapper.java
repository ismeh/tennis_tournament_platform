package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PlayerInvitationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerInvitationMapper {

    @Mapping(target = "claimedByMemberId", source = "claimedByMember")
    PlayerInvitation toDomain(PlayerInvitationEntity entity);

    @Mapping(target = "claimedByMember", source = "claimedByMemberId")
    PlayerInvitationEntity toEntity(PlayerInvitation domain);
}
