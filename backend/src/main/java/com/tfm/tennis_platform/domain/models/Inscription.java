package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Inscription {
    private final UUID id;
    private final UUID eventId;
    private final UUID participantId;
    private final String status;
    private final String paymentStatus;
    private final LocalDateTime registeredAt;
}
