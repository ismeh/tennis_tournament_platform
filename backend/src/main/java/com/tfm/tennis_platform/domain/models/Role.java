package com.tfm.tennis_platform.domain.models;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Role {
    private final UUID id;
    private final String name;
}
