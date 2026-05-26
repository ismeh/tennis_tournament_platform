package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaInscriptionRepository extends JpaRepository<InscriptionEntity, UUID> {
    List<InscriptionEntity> findByEvent_Tournament_Id(UUID tournamentId);
    List<InscriptionEntity> findByEvent_Id(UUID eventId);
    List<InscriptionEntity> findByParticipant_Id(UUID participantId);
    boolean existsByEvent_IdAndParticipant_Id(UUID eventId, UUID participantId);

    @Query("""
            select distinct inscription
            from InscriptionEntity inscription
            join fetch inscription.event event
            left join fetch event.ageCategory
            join fetch inscription.participant participant
            left join fetch participant.individualPerson
            left join fetch participant.members
            where event.tournament.id = :tournamentId
            """)
    List<InscriptionEntity> findDetailedByTournamentId(@Param("tournamentId") UUID tournamentId);

    @Query("""
            select distinct inscription
            from InscriptionEntity inscription
            join fetch inscription.event event
            left join fetch event.ageCategory
            join fetch inscription.participant participant
            left join fetch participant.individualPerson
            left join fetch participant.members
            where event.tournament.id = :tournamentId
              and event.id = :eventId
            """)
    List<InscriptionEntity> findDetailedByTournamentIdAndEventId(
            @Param("tournamentId") UUID tournamentId,
            @Param("eventId") UUID eventId
    );
}
