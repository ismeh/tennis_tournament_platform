package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.List;

public record ClubManagementResponse(
        ClubPageResponse club,
        ClubMembershipResponse membership,
        List<ClubMemberResponse> members
) {
}
