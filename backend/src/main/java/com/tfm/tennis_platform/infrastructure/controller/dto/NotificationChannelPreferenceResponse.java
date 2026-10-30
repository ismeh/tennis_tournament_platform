package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.NotificationChannel;

public record NotificationChannelPreferenceResponse(
        NotificationChannel channel
) {
}
