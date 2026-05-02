package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.DrawType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(builderClassName = "DrawBuilder", buildMethodName = "buildInternal")
public class Draw {
    private UUID id;
    private UUID stageId;
    private DrawType drawType;
    private String drawName;

    public static class DrawBuilder {
        public Draw build() {
            if (drawType == null) {
                throw new IllegalArgumentException("drawType must not be null");
            }
            if (id == null) {
                this.id = UUID.randomUUID();
            }
            return buildInternal();
        }
    }
}
