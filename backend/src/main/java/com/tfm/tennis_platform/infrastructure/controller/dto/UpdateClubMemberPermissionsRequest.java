package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.ClubPermission;

import java.util.Set;

public record UpdateClubMemberPermissionsRequest(
        Set<ClubPermission> permissions
) {
}
