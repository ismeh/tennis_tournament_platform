package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Member;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findByEmail(String email);
    Optional<Member> findById(UUID id);
    Optional<Member> findByEmailConfirmationTokenHash(String tokenHash);
    void updateTokenHash(UUID id, String tokenHash);
    void updateEmailConfirmation(UUID id, boolean emailVerified, String tokenHash, java.time.LocalDateTime expiresAt);
    void updatePersonId(UUID id, UUID personId);
    Optional<Member> findByEmailWithPersonId(String email);
    void anonymize(UUID id, String anonymizedEmail);
    void updatePrivacyConsent(UUID id, boolean accepted, java.time.LocalDateTime acceptedAt, String version);
    Optional<Member> findByRole(com.tfm.tennis_platform.domain.models.enums.UserRole role);
}
