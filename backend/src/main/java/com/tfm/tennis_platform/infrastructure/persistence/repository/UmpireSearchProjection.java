package com.tfm.tennis_platform.infrastructure.persistence.repository;

import java.util.UUID;

public interface UmpireSearchProjection {
    UUID getId();
    String getEmail();
    String getFirstName();
    String getLastName();
}
