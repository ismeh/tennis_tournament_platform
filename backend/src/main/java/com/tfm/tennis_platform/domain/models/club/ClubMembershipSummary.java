package com.tfm.tennis_platform.domain.models.club;

import com.tfm.tennis_platform.domain.models.enums.ClubPermission;
import com.tfm.tennis_platform.domain.models.enums.ClubRole;

import java.util.Set;
import java.util.UUID;

public record ClubMembershipSummary(
        UUID membershipId,
        UUID clubId,
        String name,
        String slug,
        String logoUrl,
        String primaryColor,
        String city,
        String country,
        ClubRole role,
        Set<ClubPermission> permissions,
        long memberCount
) {}
