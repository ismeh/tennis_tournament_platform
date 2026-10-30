package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Notification;
import com.tfm.tennis_platform.infrastructure.persistence.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "read", source = "read")
    @Mapping(target = "type", source = "notificationType")
    Notification toDomain(NotificationEntity entity);

    @Mapping(target = "isRead", source = "read")
    @Mapping(target = "notificationType", source = "type")
    NotificationEntity toEntity(Notification domain);
}
