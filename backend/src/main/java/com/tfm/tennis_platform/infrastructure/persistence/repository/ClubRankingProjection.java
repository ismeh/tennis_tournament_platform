package com.tfm.tennis_platform.infrastructure.persistence.repository;

import java.util.UUID;

public interface ClubRankingProjection {
    UUID getPersonId();
    String getFirstName();
    String getLastName();
    String getGender();
    String getLicense();
    Long getPoints();
    Long getVictories();
    Long getTournaments();
}
