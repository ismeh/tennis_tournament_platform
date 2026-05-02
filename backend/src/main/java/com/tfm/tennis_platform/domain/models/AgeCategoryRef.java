package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgeCategoryRef {
    private final Integer id;
    private final String category;
}
