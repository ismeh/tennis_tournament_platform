package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Notification;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationWebMapper {

    NotificationResponse toResponse(Notification domain);
}
