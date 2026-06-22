package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.ScheduleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaScheduleConfigRepository extends JpaRepository<ScheduleConfigEntity, UUID> {
    Optional<ScheduleConfigEntity> findByTournamentId(UUID tournamentId);
    void deleteByTournamentId(UUID tournamentId);
}
