package com.tfm.tennis_platform.infrastructure.controller.dto;

public record InvitationPreviewResponse(
        String tournamentName,
        String playerDisplayName,
        boolean expired,
        boolean claimed
) {
}
