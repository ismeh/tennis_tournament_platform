package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Match {
    private final UUID id;
    private final Tournament tournament;
    private final Category category;
    private final Inscription firstInscription;
    private final Inscription secondInscription;
    private final Inscription winner;
    private final Integer roundNumber;
    private final Match nextMatch;
    private final LocalDateTime scheduledAt;
    private final String court;
    private final String result;
}
