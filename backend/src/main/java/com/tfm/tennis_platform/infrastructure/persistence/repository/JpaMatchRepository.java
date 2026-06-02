package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
            select distinct match
            from MatchEntity match
            join fetch match.draw draw
            join fetch draw.stage stage
            join fetch stage.event event
            join fetch event.tournament tournament
            left join fetch event.ageCategory ageCategory
            left join fetch match.firstInscription firstInscription
            left join fetch firstInscription.participant firstParticipant
            left join fetch firstParticipant.individualPerson firstIndividualPerson
            left join firstParticipant.members firstMember
            left join fetch match.secondInscription secondInscription
            left join fetch secondInscription.participant secondParticipant
            left join fetch secondParticipant.individualPerson secondIndividualPerson
            left join secondParticipant.members secondMember
            left join fetch match.courtResource court
            where match.scheduledAt is not null
              and match.scheduledAt >= :from
              and match.scheduledAt <= :to
              and tournament.status in :statuses
              and (
                firstIndividualPerson.id = :personId
                or firstMember.id = :personId
                or secondIndividualPerson.id = :personId
                or secondMember.id = :personId
              )
            order by match.scheduledAt asc, tournament.formalName asc, match.roundNumber asc
            """)
    List<MatchEntity> findScheduledCalendarMatchesForPlayer(
            @Param("personId") UUID personId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("statuses") List<TournamentStatus> statuses
    );
}
