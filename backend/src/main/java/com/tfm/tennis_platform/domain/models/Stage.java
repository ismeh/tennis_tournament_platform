package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.StageType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(exclude = "draws")
@Builder(builderClassName = "StageBuilder", buildMethodName = "buildInternal", toBuilder = true)
public class Stage {
    private UUID id;
    private UUID eventId;
    private Integer stageNumber;
    private StageType stageType;
    private String description;
    @Singular
    private List<Draw> draws;

    public static class StageBuilder {
        public Stage build() {
            if (stageNumber == null || stageNumber <= 0) {
                throw new IllegalArgumentException("stageNumber must be greater than 0");
            }
            if (stageType == null) {
                throw new IllegalArgumentException("stageType must not be null");
            }
            if (id == null) {
                this.id = UUID.randomUUID();
            }
            if (draws == null) {
                this.draws = new ArrayList<>();
            }
            return buildInternal();
        }
    }
}
