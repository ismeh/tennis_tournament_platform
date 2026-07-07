package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
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
    private final ParticipantSource participantSource;
    private final Integer seed;
    private final Integer points;

    public boolean isProfessional() {
        return participantSource == ParticipantSource.PROFESSIONAL;
    }

    public Integer getSeedingPosition() {
        if (seed != null && seed > 0) {
            return seed;
        }

        if (points != null && points > 0) {
            return points;
        }

        return null;
    }
}
