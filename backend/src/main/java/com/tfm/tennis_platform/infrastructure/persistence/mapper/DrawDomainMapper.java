package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DrawDomainMapper {

    public Draw toDomain(DrawEntity entity) {
        if (entity == null) {
            return null;
        }

        return Draw.builder()
                .id(entity.getId())
                .stageId(entity.getStage() != null ? entity.getStage().getId() : null)
                .drawType(entity.getDrawType() != null ? DrawType.valueOf(entity.getDrawType()) : null)
                .drawName(entity.getLabel())
                .label(entity.getLabel())
                .build();
    }

    public DrawEntity toEntity(Draw domain) {
        if (domain == null) {
            return null;
        }

        return DrawEntity.builder()
                .id(domain.getId())
                .stage(mapStage(domain.getStageId()))
                .drawType(domain.getDrawType() != null ? domain.getDrawType().name() : null)
                .label(domain.getLabel() != null ? domain.getLabel() : domain.getDrawName())
                .build();
    }

    public List<Draw> toDomainList(List<DrawEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(this::toDomain).toList();
    }

    public List<DrawEntity> toEntityList(List<Draw> domains) {
        if (domains == null) {
            return List.of();
        }

        return domains.stream().map(this::toEntity).toList();
    }

    private StageEntity mapStage(UUID stageId) {
        if (stageId == null) {
            return null;
        }

        return StageEntity.builder().id(stageId).build();
    }
}