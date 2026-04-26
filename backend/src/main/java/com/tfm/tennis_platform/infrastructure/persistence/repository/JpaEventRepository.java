package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEventRepository extends JpaRepository<EventEntity, UUID> {
    Optional<EventEntity> findByIdAndTournament_Id(UUID id, UUID tournamentId);
}
