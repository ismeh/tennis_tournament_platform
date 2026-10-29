package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", updatable = false)
    private String passwordHash;

    @Column(name = "token_hash", length = 128)
    private String tokenHash;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_confirmation_token_hash", length = 128)
    private String emailConfirmationTokenHash;

    @Column(name = "email_confirmation_expires_at")
    private LocalDateTime emailConfirmationExpiresAt;

    @Enumerated(EnumType.STRING)
    private MemberTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.PLAYER;

    @Column(name = "registered_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime registeredAt;

    @Column(name = "privacy_policy_accepted", nullable = false)
    @Builder.Default
    private boolean privacyPolicyAccepted = false;

    @Column(name = "privacy_policy_accepted_at")
    private LocalDateTime privacyPolicyAcceptedAt;

    @Column(name = "privacy_policy_version", length = 10)
    private String privacyPolicyVersion;

    @Column(name = "terms_conditions_accepted", nullable = false)
    @Builder.Default
    private boolean termsConditionsAccepted = false;

    @Column(name = "terms_conditions_accepted_at")
    private LocalDateTime termsConditionsAcceptedAt;

    @Column(name = "terms_conditions_version", length = 20)
    private String termsConditionsVersion;
}
