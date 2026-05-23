package com.tfm.tennis_platform.domain.models;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Category {
    private final UUID id;
    private final String name;
    private final String genre; // 'H', 'M', 'X'
    private final String mode; // 'single', 'doubles'
}
