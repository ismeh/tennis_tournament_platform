package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Notification;
import com.tfm.tennis_platform.domain.port.out.NotificationRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.NotificationEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.NotificationMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final JpaNotificationRepository jpaNotificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = notificationMapper.toEntity(notification);
        NotificationEntity saved = jpaNotificationRepository.save(entity);
        return notificationMapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaNotificationRepository.findById(id)
                .map(notificationMapper::toDomain);
    }

    @Override
    public List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return jpaNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(UUID id) {
        jpaNotificationRepository.markAsRead(id);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        jpaNotificationRepository.markAllAsRead(userId);
    }
}
