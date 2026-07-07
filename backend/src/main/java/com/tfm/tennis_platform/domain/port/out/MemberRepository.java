package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import java.util.List;
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
    void updateTermsConsent(UUID id, boolean accepted, java.time.LocalDateTime acceptedAt, String version);
    Optional<Member> findByRole(UserRole role);
    List<Member> findAllByRole(UserRole role);
    List<Member> searchUmpiresByQuery(String query);
    List<com.tfm.tennis_platform.domain.models.UmpireSearchResult> searchUmpiresWithPersonData(String query);
    List<com.tfm.tennis_platform.domain.models.UmpireSearchResult> searchByRolesWithPersonData(List<UserRole> roles, String query);
}
