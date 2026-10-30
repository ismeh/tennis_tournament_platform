package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Notification {
    private final UUID id;
    private final UUID userId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final String linkUrl;
    private final boolean read;
    private final LocalDateTime createdAt;
}
