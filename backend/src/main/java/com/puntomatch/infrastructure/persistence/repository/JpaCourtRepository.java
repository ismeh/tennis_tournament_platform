package com.puntomatch.infrastructure.persistence.repository;

import com.puntomatch.infrastructure.persistence.entity.CourtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCourtRepository extends JpaRepository<CourtEntity, UUID> {
    List<CourtEntity> findByTournamentIdOrderByNameAsc(UUID tournamentId);
    Optional<CourtEntity> findByIdAndTournamentId(UUID id, UUID tournamentId);
    boolean existsByTournamentIdAndNameIgnoreCase(UUID tournamentId, String name);
}
