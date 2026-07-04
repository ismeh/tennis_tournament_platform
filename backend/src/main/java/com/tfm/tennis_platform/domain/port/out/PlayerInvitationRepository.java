package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.PlayerInvitation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PlayerInvitationRepository {

    PlayerInvitation save(PlayerInvitation invitation);

    Optional<PlayerInvitation> findByTokenHash(String tokenHash);

    Optional<PlayerInvitation> findByParticipantId(UUID participantId);

    void markAsClaimed(UUID invitationId, UUID memberId, LocalDateTime claimedAt);
}
