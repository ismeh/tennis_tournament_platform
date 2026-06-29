package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class TournamentUmpire {
    private final UUID id;
    private final UUID tournamentId;
    private final UUID umpireId;
    private final String umpireEmail;
    private final String umpireFirstName;
    private final String umpireLastName;
    private final LocalDateTime assignedAt;
}
