package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaMatchRepository extends JpaRepository<MatchEntity, UUID> {

    @Query("""
            select m
            from MatchEntity m
            join m.draw d
            join d.stage s
            join s.event e
            join e.tournament t
            where t.id = :tournamentId
            """)
    List<MatchEntity> findByTournamentId(UUID tournamentId);
}
