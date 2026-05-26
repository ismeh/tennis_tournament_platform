package com.tfm.tennis_platform.domain.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgeCategoryEnum {
    // Los valores se poblarán dinámicamente desde la base de datos
    // Este enum se usará como DTO, no como enum Java tradicional
    ;
    private final Integer id;
    private final String category;
}

