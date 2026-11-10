package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.ClubPermission;
import com.tfm.tennis_platform.domain.models.enums.ClubRole;

import java.util.Set;

public record ClubRolePermissionsResponse(
        ClubRole role,
        Set<ClubPermission> permissions
) {
}
