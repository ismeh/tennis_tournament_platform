package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.ScheduleConfig;

import java.util.Optional;
import java.util.UUID;

public interface ScheduleConfigRepository {
    Optional<ScheduleConfig> findByTournamentId(UUID tournamentId);
    ScheduleConfig save(ScheduleConfig scheduleConfig);
    void deleteByTournamentId(UUID tournamentId);
}
