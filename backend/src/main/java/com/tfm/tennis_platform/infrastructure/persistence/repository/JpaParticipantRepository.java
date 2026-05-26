package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaParticipantRepository extends JpaRepository<ParticipantEntity, UUID> {
    List<ParticipantEntity> findByTournamentId(UUID tournamentId);
    
    Optional<ParticipantEntity> findByTournamentIdAndIndividualPersonId(UUID tournamentId, UUID personId);
    
}
