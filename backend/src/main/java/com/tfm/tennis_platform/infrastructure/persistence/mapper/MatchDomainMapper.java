package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.MatchScore;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchSetEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.CourtEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaDrawRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCourtRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return toDomainInternal(entity);
    }

    private Match toDomainInternal(MatchEntity entity) {
        return Match.builder()
                .id(entity.getId())
                .drawId(entity.getDraw() != null ? entity.getDraw().getId() : null)
                .firstInscription(mapInscriptionDomain(entity.getFirstInscription()))
                .secondInscription(mapInscriptionDomain(entity.getSecondInscription()))
                .winner(mapInscriptionDomain(entity.getWinner()))
                .roundNumber(entity.getRoundNumber())
                .bracketPosition(entity.getBracketPosition())
                .nextMatch(mapMatchReference(entity.getNextMatch()))
                .loserNextMatch(mapMatchReference(entity.getLoserNextMatch()))
                .scheduledAt(entity.getScheduledAt())
                .scheduleTimeType(entity.getScheduleTimeType())
                .courtId(entity.getCourtResource() != null ? entity.getCourtResource().getId() : null)
                .court(entity.getCourtResource() != null ? entity.getCourtResource().getName() : entity.getCourt())
                .result(entity.getResult())
                .score(mapSetsEntityToScore(entity.getSets()))
                .notes(entity.getNotes())
                .firstPlayerPoints(entity.getFirstPlayerPoints())
                .secondPlayerPoints(entity.getSecondPlayerPoints())
                .status(entity.getStatus())
                .build();
    }

    public MatchEntity toEntity(Match domain) {
        if (domain == null) {
            return null;
        }

        MatchEntity entity = MatchEntity.builder()
                .id(domain.getId())
                .draw(mapDrawEntity(domain.getDrawId()))
                .firstInscription(mapInscriptionEntity(domain.getFirstInscription() != null ? domain.getFirstInscription().getId() : null))
                .secondInscription(mapInscriptionEntity(domain.getSecondInscription() != null ? domain.getSecondInscription().getId() : null))
                .winner(mapInscriptionEntity(domain.getWinner() != null ? domain.getWinner().getId() : null))
                .roundNumber(domain.getRoundNumber())
                .bracketPosition(domain.getBracketPosition())
                .nextMatch(null)
                .loserNextMatch(null)
                .scheduledAt(domain.getScheduledAt())
                .scheduleTimeType(domain.getScheduleTimeType())
                .courtResource(mapCourtEntity(domain.getCourtId()))
                .court(domain.getCourt())
                .result(domain.getResult())
                .notes(domain.getNotes())
                .firstPlayerPoints(domain.getFirstPlayerPoints())
                .secondPlayerPoints(domain.getSecondPlayerPoints())
                .status(domain.getStatus())
                .build();

        if (domain.getScore() != null && domain.getScore().getSets() != null) {
            MatchEntity existingEntity = matchRepository.findById(domain.getId()).orElse(null);
            List<MatchSetEntity> sets = domain.getScore().getSets().stream()
                    .map(set -> MatchSetEntity.builder()
                            .id(findExistingSetId(existingEntity, set.getSetNumber()))
                            .match(entity)
                            .setNumber(set.getSetNumber())
                            .firstPlayerGames(set.getFirstPlayerGames())
                            .secondPlayerGames(set.getSecondPlayerGames())
                            .firstPlayerTiebreak(set.getFirstPlayerTiebreak())
                            .secondPlayerTiebreak(set.getSecondPlayerTiebreak())
                            .build())
                    .collect(Collectors.toList());
            entity.setSets(sets);
        }

        return entity;
    }

    private UUID findExistingSetId(MatchEntity matchEntity, int setNumber) {
        if (matchEntity != null && matchEntity.getSets() != null) {
            for (MatchSetEntity existing : matchEntity.getSets()) {
                if (existing.getSetNumber() != null && existing.getSetNumber() == setNumber) {
                    return existing.getId();
                }
            }
        }
        return UUID.randomUUID();
    }

    private MatchScore mapSetsEntityToScore(List<MatchSetEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return MatchScore.empty();
        }
        List<com.tfm.tennis_platform.domain.models.SetScore> sets = entities.stream()
                .map(entity -> com.tfm.tennis_platform.domain.models.SetScore.builder()
                        .setNumber(entity.getSetNumber())
                        .firstPlayerGames(entity.getFirstPlayerGames())
                        .secondPlayerGames(entity.getSecondPlayerGames())
                        .firstPlayerTiebreak(entity.getFirstPlayerTiebreak())
                        .secondPlayerTiebreak(entity.getSecondPlayerTiebreak())
                        .build())
                .sorted(java.util.Comparator.comparingInt(com.tfm.tennis_platform.domain.models.SetScore::getSetNumber))
                .toList();
        return MatchScore.builder().sets(sets).build();
    }

    public MatchEntity toEntityWithoutNextMatch(Match domain) {
        return toEntity(domain);
    }

    public List<Match> toDomainList(List<MatchEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomainInternal)
                .toList();
    }

    private Match mapMatchReference(MatchEntity entity) {
        if (entity == null) {
            return null;
        }
        return Match.builder()
                .id(entity.getId())
                .build();
    }

    private Inscription mapInscriptionDomain(InscriptionEntity entity) {
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

    private CourtEntity mapCourtEntity(UUID courtId) {
        if (courtId == null) {
            return null;
        }
        return courtRepository.getReferenceById(courtId);
    }
}
