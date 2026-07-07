package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import org.springframework.stereotype.Component;

@Component
public class InscriptionMapper {

    public Inscription toDomain(InscriptionEntity entity) {
        if (entity == null) {
            return null;
        }

        ParticipantEntity participant = entity.getParticipant();
        return Inscription.builder()
                .id(entity.getId())
                .eventId(entity.getEvent() != null ? entity.getEvent().getId() : null)
                .participantId(participant != null ? participant.getId() : null)
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .registeredAt(entity.getRegisteredAt())
                .participantSource(participant != null ? participant.getParticipantSource() : null)
                .seed(participant != null ? participant.getSeed() : null)
                .points(participant != null ? participant.getPoints() : null)
                .build();
    }

    public InscriptionEntity toEntity(Inscription domain) {
        if (domain == null) {
            return null;
        }

        return InscriptionEntity.builder()
                .id(domain.getId())
                .event(domain.getEventId() != null ? EventEntity.builder().id(domain.getEventId()).build() : null)
                .participant(domain.getParticipantId() != null ? ParticipantEntity.builder().id(domain.getParticipantId()).build() : null)
                .status(domain.getStatus())
                .paymentStatus(domain.getPaymentStatus())
                .registeredAt(domain.getRegisteredAt())
                .build();
    }
}
