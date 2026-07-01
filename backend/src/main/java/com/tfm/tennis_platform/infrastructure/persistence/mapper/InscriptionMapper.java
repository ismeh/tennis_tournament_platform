package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InscriptionMapper {

    private final JpaProPlayerRepository proPlayerRepository;

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
                .professionalRankingPosition(resolveProfessionalRankingPosition(participant))
                .professionalAwardedPoints(resolveProfessionalAwardedPoints(participant))
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

    private Integer resolveProfessionalRankingPosition(ParticipantEntity participant) {
        if (participant == null || participant.getParticipantSource() != ParticipantSource.PROFESSIONAL) {
            return null;
        }

        String license = participant.getDisplayTennisId();
        if (license == null || license.isBlank()) {
            return null;
        }

        return proPlayerRepository.findFirstByLicenseIgnoreCase(license.trim())
                .map(proPlayer -> proPlayer.getRankingPosition())
                .orElse(null);
    }

    private Integer resolveProfessionalAwardedPoints(ParticipantEntity participant) {
        if (participant == null || participant.getParticipantSource() != ParticipantSource.PROFESSIONAL) {
            return null;
        }

        String license = participant.getDisplayTennisId();
        if (license == null || license.isBlank()) {
            return null;
        }

        return proPlayerRepository.findFirstByLicenseIgnoreCase(license.trim())
                .map(proPlayer -> proPlayer.getAwardedPoints())
                .orElse(null);
    }
}
