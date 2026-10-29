package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Stage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StageRepository {
    Stage save(Stage stage);

    Optional<Stage> findById(UUID id);

    List<Stage> findByEventId(UUID eventId);
}