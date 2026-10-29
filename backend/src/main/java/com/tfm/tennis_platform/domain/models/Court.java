package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Court {
    private final UUID id;
    private final UUID tournamentId;
    private final String name;
    private final boolean active;
}
