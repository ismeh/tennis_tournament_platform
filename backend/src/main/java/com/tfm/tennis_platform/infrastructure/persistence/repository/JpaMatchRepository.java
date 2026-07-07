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
            select distinct m
            from MatchEntity m
            join fetch m.draw d
            join fetch d.stage s
            join fetch s.event e
            join fetch e.tournament t
            left join fetch m.firstInscription firstInscription
            left join fetch firstInscription.participant firstParticipant
            left join fetch m.secondInscription secondInscription
            left join fetch secondInscription.participant secondParticipant
            left join fetch m.winner winner
            left join fetch winner.participant winnerParticipant
            left join fetch m.courtResource court
            where t.id = :tournamentId
            order by m.scheduledAt asc nulls last, s.order asc, d.id asc, m.roundNumber asc, m.bracketPosition asc, m.id asc
            """)
    List<MatchEntity> findByTournamentId(UUID tournamentId);

    @Query("""
            select distinct m
            from MatchEntity m
            join fetch m.draw d
            join fetch d.stage s
            join fetch s.event e
            join fetch e.tournament t
            left join fetch m.firstInscription firstInscription
            left join fetch firstInscription.participant firstParticipant
            left join fetch m.secondInscription secondInscription
            left join fetch secondInscription.participant secondParticipant
            left join fetch m.winner winner
            left join fetch winner.participant winnerParticipant
            left join fetch m.nextMatch nextMatch
            left join fetch m.loserNextMatch loserNextMatch
            left join fetch m.courtResource court
            where m.id = :matchId
              and t.id = :tournamentId
            """)
    java.util.Optional<MatchEntity> findByIdAndTournamentId(
            @Param("matchId") UUID matchId,
            @Param("tournamentId") UUID tournamentId
    );

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

    @Query("""
            select
              participant.id as participantId,
              participant.displayTennisId as license,
              participant.displayFirstName as firstName,
              participant.displayLastName as lastName,
              participant.displayGender as gender,
              sum(case
                when winner.id = firstInscription.id then cast(coalesce(match.firstPlayerPoints, '0') as long)
                when winner.id = secondInscription.id then cast(coalesce(match.secondPlayerPoints, '0') as long)
                else 0
              end) as points,
              count(match.id) as victories
            from MatchEntity match
            join match.draw draw
            join draw.stage stage
            join stage.event event
            left join event.ageCategory ageCategory
            join match.winner winner
            join winner.participant participant
            left join match.firstInscription firstInscription
            left join match.secondInscription secondInscription
            where event.tournament.id = :tournamentId
              and match.status = com.tfm.tennis_platform.domain.models.enums.MatchStatus.COMPLETED
              and (:gender is null or upper(coalesce(event.gender, '')) = :gender)
              and (:categoryId is null or ageCategory.id = :categoryId)
            group by
              participant.id,
              participant.displayTennisId,
              participant.displayFirstName,
              participant.displayLastName,
              participant.displayGender
            order by
              sum(case
                when winner.id = firstInscription.id then cast(coalesce(match.firstPlayerPoints, '0') as long)
                when winner.id = secondInscription.id then cast(coalesce(match.secondPlayerPoints, '0') as long)
                else 0
              end) desc,
              count(match.id) desc,
              participant.displayLastName asc,
              participant.displayFirstName asc
            """)
    List<TournamentRankingProjection> findTournamentRanking(
            @Param("tournamentId") UUID tournamentId,
            @Param("gender") String gender,
            @Param("categoryId") Integer categoryId
    );
}
