package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Member {
    private final UUID id;
    private final String email;
    private final String username;
    private final String password;
    private final String tokenHash;
    private final boolean emailVerified;
    private final String emailConfirmationTokenHash;
    private final LocalDateTime emailConfirmationExpiresAt;
    private final String gender; // 'H' or 'M'
    private final MemberTier tier;
    private final LocalDateTime registeredAt;
    private final UUID personId;

    public Member withEmailConfirmation(String tokenHash, LocalDateTime expiresAt) {
        return toBuilder()
                .emailVerified(false)
                .emailConfirmationTokenHash(tokenHash)
                .emailConfirmationExpiresAt(expiresAt)
                .build();
    }

    public Member confirmEmail() {
        return toBuilder()
                .emailVerified(true)
                .emailConfirmationTokenHash(null)
                .emailConfirmationExpiresAt(null)
                .build();
    }
}
