package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Category;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.persistence.entity.CategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class MatchDomainMapper {

    public Match toDomain(MatchEntity entity) {
        if (entity == null) {
            return null;
        }

        return Match.builder()
                .id(entity.getId())
                .tournament(mapTournamentDomain(entity.getTournament() != null ? entity.getTournament().getId() : null))
                .category(mapCategoryDomain(entity.getCategory() != null ? entity.getCategory().getId() : null))
                .drawId(entity.getDraw() != null ? entity.getDraw().getId() : null)
                .firstInscription(mapInscriptionDomain(entity.getFirstInscription() != null ? entity.getFirstInscription().getId() : null))
                .secondInscription(mapInscriptionDomain(entity.getSecondInscription() != null ? entity.getSecondInscription().getId() : null))
                .winner(mapInscriptionDomain(entity.getWinner() != null ? entity.getWinner().getId() : null))
                .roundNumber(entity.getRoundNumber())
                .nextMatch(toDomain(entity.getNextMatch()))
                .scheduledAt(entity.getScheduledAt())
                .court(entity.getCourt())
                .result(entity.getResult())
                .build();
    }

    public MatchEntity toEntity(Match domain) {
        if (domain == null) {
            return null;
        }

        return MatchEntity.builder()
                .id(domain.getId())
                .tournament(mapTournamentEntity(domain.getTournament() != null ? domain.getTournament().getId() : null))
                .category(mapCategoryEntity(domain.getCategory() != null ? domain.getCategory().getId() : null))
                .draw(mapDrawEntity(domain.getDrawId()))
                .firstInscription(mapInscriptionEntity(domain.getFirstInscription() != null ? domain.getFirstInscription().getId() : null))
                .secondInscription(mapInscriptionEntity(domain.getSecondInscription() != null ? domain.getSecondInscription().getId() : null))
                .winner(mapInscriptionEntity(domain.getWinner() != null ? domain.getWinner().getId() : null))
                .roundNumber(domain.getRoundNumber())
                .nextMatch(toEntity(domain.getNextMatch()))
                .scheduledAt(domain.getScheduledAt())
                .court(domain.getCourt())
                .result(domain.getResult())
                .build();
    }

    public List<Match> toDomainList(List<MatchEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(this::toDomain).toList();
    }

    private Tournament mapTournamentDomain(UUID tournamentId) {
        if (tournamentId == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        return Tournament.builder()
                .id(tournamentId)
                .name("Match tournament")
                .playPeriod(new TournamentPeriod(today, today.plusDays(1)))
                .inscriptionPeriod(new TournamentPeriod(today, today.plusDays(1)))
                .surface(Surface.HARD)
                .maxPlayers(2)
                .location("N/A")
                .state(TournamentStatus.DRAFT)
                .build();
    }

    private Category mapCategoryDomain(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return Category.builder().id(categoryId).build();
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

    private TournamentEntity mapTournamentEntity(UUID tournamentId) {
        if (tournamentId == null) {
            return null;
        }

        return TournamentEntity.builder().id(tournamentId).build();
    }

    private CategoryEntity mapCategoryEntity(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return CategoryEntity.builder().id(categoryId).build();
    }

    private DrawEntity mapDrawEntity(UUID drawId) {
        if (drawId == null) {
            return null;
        }

        return DrawEntity.builder().id(drawId).build();
    }

    private InscriptionEntity mapInscriptionEntity(UUID inscriptionId) {
        if (inscriptionId == null) {
            return null;
        }

        return InscriptionEntity.builder().id(inscriptionId).build();
    }
}