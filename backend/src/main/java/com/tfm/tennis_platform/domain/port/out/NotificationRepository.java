package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countUnreadByUserId(UUID userId);

    void markAsRead(UUID id);

    void markAllAsRead(UUID userId);
}
