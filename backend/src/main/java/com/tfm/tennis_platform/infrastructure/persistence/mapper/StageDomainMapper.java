package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class StageDomainMapper {

    private final DrawDomainMapper drawDomainMapper;

    public StageDomainMapper(DrawDomainMapper drawDomainMapper) {
        this.drawDomainMapper = drawDomainMapper;
    }

    public Stage toDomain(StageEntity entity) {
        if (entity == null) {
            return null;
        }

        return Stage.builder()
                .id(entity.getId())
                .eventId(entity.getEvent() != null ? entity.getEvent().getId() : null)
                .stageNumber(entity.getOrder())
                .stageType(entity.getStageType() != null ? StageType.valueOf(entity.getStageType()) : null)
                .strategyName(entity.getStrategyName())
                .description(entity.getDescription())
                .draws(toDomainDraws(entity.getDraws()))
                .build();
    }

    public StageEntity toEntity(Stage domain) {
        if (domain == null) {
            return null;
        }

        StageEntity entity = StageEntity.builder()
                .id(domain.getId())
                .event(mapEvent(domain.getEventId()))
                .order(domain.getStageNumber())
                .stageType(domain.getStageType() != null ? domain.getStageType().name() : null)
                .strategyName(domain.getStrategyName())
                .description(domain.getDescription())
                .draws(new ArrayList<>())
                .build();

        if (domain.getDraws() != null) {
            List<DrawEntity> draws = domain.getDraws().stream()
                    .map(drawDomainMapper::toEntity)
                    .peek(drawEntity -> drawEntity.setStage(entity))
                    .toList();
            entity.getDraws().addAll(draws);
        }

        return entity;
    }

    public List<Stage> toDomainList(List<StageEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(this::toDomain).toList();
    }

    public List<StageEntity> toEntityList(List<Stage> domains) {
        if (domains == null) {
            return List.of();
        }

        return domains.stream().map(this::toEntity).toList();
    }

    private EventEntity mapEvent(UUID eventId) {
        if (eventId == null) {
            return null;
        }

        return EventEntity.builder().id(eventId).build();
    }

    private List<Draw> toDomainDraws(List<DrawEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(drawDomainMapper::toDomain)
                .toList();
    }
}