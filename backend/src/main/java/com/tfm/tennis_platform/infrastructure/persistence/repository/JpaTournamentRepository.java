package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface JpaTournamentRepository extends JpaRepository<TournamentEntity, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE TournamentEntity t SET t.createdBy.id = :newOwnerId WHERE t.createdBy.id = :oldOwnerId")
    int transferTournaments(@Param("oldOwnerId") UUID oldOwnerId, @Param("newOwnerId") UUID newOwnerId);
}
