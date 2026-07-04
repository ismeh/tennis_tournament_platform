package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.ClaimInvitationService;
import com.tfm.tennis_platform.application.services.InviteParticipantService;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClaimInvitationRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClaimInvitationResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.InvitationPreviewResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.InviteParticipantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InviteParticipantService inviteParticipantService;
    private final ClaimInvitationService claimInvitationService;

    @PostMapping("/api/tournaments/{tournamentId}/participants/{participantId}/invite")
    public ResponseEntity<InviteParticipantResponse> generateInvitation(
            @PathVariable UUID tournamentId,
            @PathVariable UUID participantId,
            Principal principal
    ) {
        String invitationUrl = inviteParticipantService.generateInvitationUrl(
                tournamentId, participantId, principal.getName());
        return ResponseEntity.ok(new InviteParticipantResponse(invitationUrl));
    }

    @GetMapping("/api/invitations/preview")
    public ResponseEntity<InvitationPreviewResponse> previewInvitation(@RequestParam String token) {
        ClaimInvitationService.InvitationPreview preview = claimInvitationService.previewInvitation(token);
        return ResponseEntity.ok(new InvitationPreviewResponse(
                preview.tournamentName(),
                preview.playerDisplayName(),
                preview.expired(),
                preview.claimed()
        ));
    }

    @PostMapping("/api/auth/claim-invitation")
    public ResponseEntity<ClaimInvitationResponse> claimInvitation(
            @RequestBody ClaimInvitationRequest request,
            Principal principal
    ) {
        claimInvitationService.claimInvitation(request.token(), principal.getName());
        return ResponseEntity.ok(new ClaimInvitationResponse("Vinculación completada. Ya apareces en el torneo con tu cuenta."));
    }
}
