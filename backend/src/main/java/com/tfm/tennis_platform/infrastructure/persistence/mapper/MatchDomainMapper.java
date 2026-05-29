package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.CourtEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaDrawRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCourtRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MatchDomainMapper {

    private final JpaDrawRepository drawRepository;
    private final JpaInscriptionRepository inscriptionRepository;
    private final JpaMatchRepository matchRepository;
    private final JpaCourtRepository courtRepository;

    public Match toDomain(MatchEntity entity) {
        if (entity == null) {
            return null;
        }

        return Match.builder()
                .id(entity.getId())
                .drawId(entity.getDraw() != null ? entity.getDraw().getId() : null)
                .firstInscription(mapInscriptionDomain(entity.getFirstInscription() != null ? entity.getFirstInscription().getId() : null))
                .secondInscription(mapInscriptionDomain(entity.getSecondInscription() != null ? entity.getSecondInscription().getId() : null))
                .winner(mapInscriptionDomain(entity.getWinner() != null ? entity.getWinner().getId() : null))
                .roundNumber(entity.getRoundNumber())
                .nextMatch(toDomain(entity.getNextMatch()))
                .scheduledAt(entity.getScheduledAt())
                .scheduleTimeType(entity.getScheduleTimeType())
                .courtId(entity.getCourtResource() != null ? entity.getCourtResource().getId() : null)
                .court(entity.getCourtResource() != null ? entity.getCourtResource().getName() : entity.getCourt())
                .result(entity.getResult())
                .build();
    }

    public MatchEntity toEntity(Match domain) {
        if (domain == null) {
            return null;
        }

        // Note: do not map nextMatch here to avoid creating multiple MatchEntity instances
        // with the same identifier in the same persistence context when saving batches.
        return MatchEntity.builder()
                .id(domain.getId())
                .draw(mapDrawEntity(domain.getDrawId()))
                .firstInscription(mapInscriptionEntity(domain.getFirstInscription() != null ? domain.getFirstInscription().getId() : null))
                .secondInscription(mapInscriptionEntity(domain.getSecondInscription() != null ? domain.getSecondInscription().getId() : null))
                .winner(mapInscriptionEntity(domain.getWinner() != null ? domain.getWinner().getId() : null))
                .roundNumber(domain.getRoundNumber())
                .nextMatch(null)
                .scheduledAt(domain.getScheduledAt())
                .scheduleTimeType(domain.getScheduleTimeType())
                .courtResource(mapCourtEntity(domain.getCourtId()))
                .court(domain.getCourt())
                .result(domain.getResult())
                .build();
    }

    /**
     * Create an entity without wiring the nextMatch relationship. Adapter will link nextMatch instances
     * to ensure the same MatchEntity object is reused within a saveAll operation.
     */
    public MatchEntity toEntityWithoutNextMatch(Match domain) {
        return toEntity(domain);
    }

    public List<Match> toDomainList(List<MatchEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(this::toDomain).toList();
    }

    private Inscription mapInscriptionDomain(UUID inscriptionId) {
        if (inscriptionId == null) {
            return null;
        }

        return Inscription.builder()
                .id(inscriptionId)
                .eventId(null)
                .participantId(null)
                .status(null)
                .paymentStatus(null)
                .registeredAt(null)
                .build();
    }

    private DrawEntity mapDrawEntity(UUID drawId) {
        if (drawId == null) {
            return null;
        }

        return drawRepository.getReferenceById(drawId);
    }

    private InscriptionEntity mapInscriptionEntity(UUID inscriptionId) {
        if (inscriptionId == null) {
            return null;
        }

        return inscriptionRepository.getReferenceById(inscriptionId);
    }

    private MatchEntity mapMatchEntity(Match nextMatch) {
        if (nextMatch == null || nextMatch.getId() == null) {
            return null;
        }

        return matchRepository.getReferenceById(nextMatch.getId());
    }

    private CourtEntity mapCourtEntity(UUID courtId) {
        if (courtId == null) {
            return null;
        }

        return courtRepository.getReferenceById(courtId);
    }
}
