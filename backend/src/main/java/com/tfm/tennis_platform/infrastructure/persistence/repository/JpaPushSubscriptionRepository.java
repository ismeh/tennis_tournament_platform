package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    List<PushSubscriptionEntity> findByUserId(UUID userId);

    void deleteByUserIdAndEndpoint(UUID userId, String endpoint);

    @Modifying
    @Transactional
    @Query("UPDATE PushSubscriptionEntity s SET s.lastUsedAt = :lastUsedAt WHERE s.id = :id")
    int updateLastUsedAt(@Param("id") UUID id, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
