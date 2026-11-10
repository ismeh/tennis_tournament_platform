package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.ClubPermission;
import com.tfm.tennis_platform.domain.models.enums.ClubRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ClubMemberResponse(
        UUID membershipId,
        UUID memberId,
        String email,
        String firstName,
        String lastName,
        ClubRole role,
        Set<ClubPermission> customPermissions,
        Set<ClubPermission> effectivePermissions,
        LocalDateTime joinedAt
) {
}
