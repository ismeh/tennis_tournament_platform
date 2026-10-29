package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class PlayerInvitation {

    private final UUID id;
    private final UUID participantId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime claimedAt;
    private final UUID claimedByMemberId;
    private final LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isClaimed() {
        return claimedAt != null;
    }

    public PlayerInvitation claim(UUID memberId) {
        return toBuilder()
                .claimedAt(LocalDateTime.now())
                .claimedByMemberId(memberId)
                .build();
    }
}
