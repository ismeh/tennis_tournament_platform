package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.MatchScore;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchSetEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.CourtEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaDrawRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCourtRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MatchDomainMapper {

    private final JpaDrawRepository drawRepository;
    private final JpaInscriptionRepository inscriptionRepository;
    private final JpaMatchRepository matchRepository;
    private final JpaCourtRepository courtRepository;
    private final JpaProPlayerRepository proPlayerRepository;

    public Match toDomain(MatchEntity entity) {
        if (entity == null) {
            return null;
        }

        Map<String, ProPlayerData> professionalDataByLicense = loadProfessionalData(List.of(entity));
        return toDomain(entity, professionalDataByLicense);
    }

    private Match toDomain(MatchEntity entity, Map<String, ProPlayerData> professionalDataByLicense) {
        return Match.builder()
                .id(entity.getId())
                .drawId(entity.getDraw() != null ? entity.getDraw().getId() : null)
                .firstInscription(mapInscriptionDomain(entity.getFirstInscription(), professionalDataByLicense))
                .secondInscription(mapInscriptionDomain(entity.getSecondInscription(), professionalDataByLicense))
                .winner(mapInscriptionDomain(entity.getWinner(), professionalDataByLicense))
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

        // Note: do not map nextMatch here to avoid creating multiple MatchEntity instances
        // with the same identifier in the same persistence context when saving batches.
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

        Map<String, ProPlayerData> professionalDataByLicense = loadProfessionalData(entities);
        return entities.stream()
                .map(entity -> toDomain(entity, professionalDataByLicense))
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

    private Inscription mapInscriptionDomain(InscriptionEntity entity, Map<String, ProPlayerData> professionalDataByLicense) {
        if (entity == null) {
            return null;
        }

        ParticipantEntity participant = entity.getParticipant();
        ParticipantSource participantSource = participant != null ? participant.getParticipantSource() : null;
        ProPlayerData professionalData = findProfessionalData(participant, professionalDataByLicense);
        return Inscription.builder()
                .id(entity.getId())
                .eventId(entity.getEvent() != null ? entity.getEvent().getId() : null)
                .participantId(participant != null ? participant.getId() : null)
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .registeredAt(entity.getRegisteredAt())
                .participantSource(participantSource)
                .seed(participant != null ? participant.getSeed() : null)
                .professionalRankingPosition(professionalData != null ? professionalData.rankingPosition() : null)
                .professionalAwardedPoints(professionalData != null ? professionalData.awardedPoints() : null)
                .build();
    }

    private ProPlayerData findProfessionalData(ParticipantEntity participant, Map<String, ProPlayerData> professionalDataByLicense) {
        if (participant == null || participant.getParticipantSource() != ParticipantSource.PROFESSIONAL) {
            return null;
        }

        String normalized = normalizeLicense(participant.getDisplayTennisId());
        return normalized != null ? professionalDataByLicense.get(normalized) : null;
    }

    private Map<String, ProPlayerData> loadProfessionalData(List<MatchEntity> matches) {
        List<String> licenses = matches.stream()
                .filter(Objects::nonNull)
                .flatMap(this::inscriptions)
                .filter(Objects::nonNull)
                .map(InscriptionEntity::getParticipant)
                .filter(Objects::nonNull)
                .filter(participant -> participant.getParticipantSource() == ParticipantSource.PROFESSIONAL)
                .map(ParticipantEntity::getDisplayTennisId)
                .map(this::normalizeLicense)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (licenses.isEmpty()) {
            return Map.of();
        }

        return proPlayerRepository.findByNormalizedLicenses(licenses).stream()
                .filter(proPlayer -> normalizeLicense(proPlayer.getLicense()) != null)
                .collect(Collectors.toMap(
                        proPlayer -> normalizeLicense(proPlayer.getLicense()),
                        ProPlayerData::from,
                        (left, right) -> left
                ));
    }

    private Stream<InscriptionEntity> inscriptions(MatchEntity match) {
        return Stream.of(match.getFirstInscription(), match.getSecondInscription(), match.getWinner());
    }

    private String normalizeLicense(String license) {
        if (license == null || license.isBlank()) {
            return null;
        }

        return license.trim().toLowerCase(Locale.ROOT);
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

    private record ProPlayerData(Integer rankingPosition, Integer awardedPoints) {
        private static ProPlayerData from(ProPlayerEntity entity) {
            return new ProPlayerData(entity.getRankingPosition(), entity.getAwardedPoints());
        }
    }
}
