package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.PlayerInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPlayerInvitationRepository extends JpaRepository<PlayerInvitationEntity, UUID> {

    Optional<PlayerInvitationEntity> findByTokenHash(String tokenHash);

    Optional<PlayerInvitationEntity> findByParticipantId(UUID participantId);

    @Modifying
    @Query("UPDATE PlayerInvitationEntity p SET p.claimedAt = :claimedAt, p.claimedByMember = :memberId WHERE p.id = :id")
    void markAsClaimed(@Param("id") UUID id, @Param("memberId") UUID memberId, @Param("claimedAt") LocalDateTime claimedAt);
}
