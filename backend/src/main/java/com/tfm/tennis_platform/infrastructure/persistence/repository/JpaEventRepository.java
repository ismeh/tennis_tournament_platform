package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEventRepository extends JpaRepository<EventEntity, UUID> {
    Optional<EventEntity> findByIdAndTournament_Id(UUID id, UUID tournamentId);

    @Query("""
            select event
            from EventEntity event
            left join fetch event.ageCategory
            where event.tournament.id = :tournamentId
            order by event.ageCategory.category asc, event.gender asc, event.name asc
            """)
    List<EventEntity> findAllByTournamentId(@Param("tournamentId") UUID tournamentId);
}
