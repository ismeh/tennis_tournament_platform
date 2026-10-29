package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentUmpireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTournamentUmpireRepository extends JpaRepository<TournamentUmpireEntity, UUID> {

    List<TournamentUmpireEntity> findByTournamentIdOrderByAssignedAtAsc(UUID tournamentId);

    Optional<TournamentUmpireEntity> findByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId);

    boolean existsByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId);

    @Modifying
    @Transactional
    void deleteByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId);

    @Query("""
            SELECT tu.umpire.id
            FROM TournamentUmpireEntity tu
            WHERE tu.tournament.id = :tournamentId
            """)
    List<UUID> findUmpireIdsByTournamentId(@Param("tournamentId") UUID tournamentId);
}
