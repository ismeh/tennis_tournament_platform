package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UmpireSearchResult {
    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
}
