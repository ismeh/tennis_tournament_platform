package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;


@Repository
public interface JpaMemberRepository extends JpaRepository<MemberEntity, UUID> {

    Optional<MemberEntity> findByEmail(String username);

    Optional<MemberEntity> findByEmailConfirmationTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE MemberEntity m SET m.tokenHash = :tokenHash WHERE m.id = :id")
    int updateTokenHash(@Param("id") UUID id, @Param("tokenHash") String tokenHash);

    @Modifying
    @Transactional
    @Query("""
            UPDATE MemberEntity m
            SET m.emailVerified = :emailVerified,
                m.emailConfirmationTokenHash = :tokenHash,
                m.emailConfirmationExpiresAt = :expiresAt
            WHERE m.id = :id
            """)
    int updateEmailConfirmation(
            @Param("id") UUID id,
            @Param("emailVerified") boolean emailVerified,
            @Param("tokenHash") String tokenHash,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Modifying
    @Transactional
    @Query("UPDATE MemberEntity m SET m.personId = :personId WHERE m.id = :id")
    int updatePersonId(@Param("id") UUID id, @Param("personId") UUID personId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE MemberEntity m
            SET m.email = :anonymizedEmail,
                m.passwordHash = 'DELETED',
                m.tokenHash = NULL,
                m.emailConfirmationTokenHash = NULL,
                m.emailConfirmationExpiresAt = NULL,
                m.personId = NULL
            WHERE m.id = :id
            """)
    int anonymize(@Param("id") UUID id, @Param("anonymizedEmail") String anonymizedEmail);

    @Modifying
    @Transactional
    @Query("""
            UPDATE MemberEntity m
            SET m.privacyPolicyAccepted = :accepted,
                m.privacyPolicyAcceptedAt = :acceptedAt,
                m.privacyPolicyVersion = :version
            WHERE m.id = :id
            """)
    int updatePrivacyConsent(
            @Param("id") UUID id,
            @Param("accepted") boolean accepted,
            @Param("acceptedAt") LocalDateTime acceptedAt,
            @Param("version") String version
    );

    Optional<MemberEntity> findFirstByRole(UserRole role);

}
