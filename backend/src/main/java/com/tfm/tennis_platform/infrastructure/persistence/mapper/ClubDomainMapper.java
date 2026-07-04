package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import org.springframework.stereotype.Component;

@Component
public class ClubDomainMapper {

    public Club toDomain(ClubEntity entity) {
        if (entity == null) {
            return null;
        }
        return Club.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public ClubEntity toEntity(Club domain) {
        if (domain == null) {
            return null;
        }
        return ClubEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .build();
    }
}
