package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Draw;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawRepository {
    Draw save(Draw draw);

    Optional<Draw> findById(UUID id);

    List<Draw> findByStageId(UUID stageId);
}