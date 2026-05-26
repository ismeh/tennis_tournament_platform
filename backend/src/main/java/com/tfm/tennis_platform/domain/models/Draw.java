package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.DrawType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "DrawBuilder", buildMethodName = "buildInternal", toBuilder = true)
public class Draw {
    private UUID id;
    private UUID stageId;
    private DrawType drawType;
    private String drawName;
    private String label;
    @Builder.Default
    private List<Match> matches = new ArrayList<>();

    public static class DrawBuilder {
        public Draw build() {
            if (drawType == null) {
                throw new IllegalArgumentException("drawType must not be null");
            }
            if (id == null) {
                this.id = UUID.randomUUID();
            }
            Draw draw = buildInternal();
            if (draw.matches == null) {
                draw.matches = new ArrayList<>();
            }
            return draw;
        }
    }
}
