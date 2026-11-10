package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.PushSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository {

    PushSubscription save(PushSubscription subscription);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserId(UUID userId);

    void deleteById(UUID id);

    void deleteByUserIdAndEndpoint(UUID userId, String endpoint);

    void updateLastUsedAt(UUID id, LocalDateTime lastUsedAt);
}
