package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.ClubRole;

public record UpdateClubMemberRoleRequest(
        ClubRole role
) {
}
