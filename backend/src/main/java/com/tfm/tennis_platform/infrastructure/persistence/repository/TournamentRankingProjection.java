package com.tfm.tennis_platform.infrastructure.persistence.repository;

import java.util.UUID;

public interface TournamentRankingProjection {
    UUID getParticipantId();
    String getLicense();
    String getFirstName();
    String getLastName();
    String getGender();
    Long getPoints();
    Long getVictories();
}
