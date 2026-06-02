package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.infrastructure.persistence.entity.CourtEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourtDomainMapper {

    private final JpaTournamentRepository tournamentRepository;

    public Court toDomain(CourtEntity entity) {
        if (entity == null) {
            return null;
        }

        return Court.builder()
                .id(entity.getId())
                .tournamentId(entity.getTournament() != null ? entity.getTournament().getId() : null)
                .name(entity.getName())
                .active(entity.isActive())
                .build();
    }

    public CourtEntity toEntity(Court domain) {
        if (domain == null) {
            return null;
        }

        return CourtEntity.builder()
                .id(domain.getId())
                .tournament(tournamentRepository.getReferenceById(domain.getTournamentId()))
                .name(domain.getName())
                .active(domain.isActive())
                .build();
    }
}
