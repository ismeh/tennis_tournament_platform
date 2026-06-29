package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
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
    private final String gender;
    private final MemberTier tier;
    private final UserRole role;
    private final LocalDateTime registeredAt;
    private final UUID personId;
    private final boolean privacyPolicyAccepted;
    private final LocalDateTime privacyPolicyAcceptedAt;
    private final String privacyPolicyVersion;
    private final boolean termsConditionsAccepted;
    private final LocalDateTime termsConditionsAcceptedAt;
    private final String termsConditionsVersion;

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

    public Member withPrivacyConsent(String version) {
        return toBuilder()
                .privacyPolicyAccepted(true)
                .privacyPolicyAcceptedAt(LocalDateTime.now())
                .privacyPolicyVersion(version)
                .build();
    }

    public Member withTermsConsent(String version) {
        return toBuilder()
                .termsConditionsAccepted(true)
                .termsConditionsAcceptedAt(LocalDateTime.now())
                .termsConditionsVersion(version)
                .build();
    }
}
