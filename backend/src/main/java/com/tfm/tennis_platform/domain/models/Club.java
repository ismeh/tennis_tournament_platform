package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Club {
    private final UUID id;
    private final String name;
}
