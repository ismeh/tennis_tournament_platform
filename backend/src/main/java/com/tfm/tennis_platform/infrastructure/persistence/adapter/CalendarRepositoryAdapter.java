package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CalendarRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CalendarRepositoryAdapter implements CalendarRepository {

    private final EntityManager entityManager;
    private final JpaMatchRepository matchRepository;
    private final JpaMemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TournamentCalendarItem> findPublishedTournaments(
            LocalDate from,
            LocalDate to,
            List<TournamentStatus> statuses,
            Surface surface,
            String location
    ) {
        return findTournamentEntities(from, to, statuses, surface, location).stream()
                .map(this::toTournamentCalendarItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerMatchCalendarItem> findScheduledMatchesForPlayer(
            String playerEmail,
            LocalDateTime from,
            LocalDateTime to,
            List<TournamentStatus> statuses
    ) {
        return memberRepository.findByEmail(playerEmail)
                .map(member -> member.getPersonId() == null
                        ? List.<PlayerMatchCalendarItem>of()
                        : findMatchesForPerson(member.getPersonId(), from, to, statuses))
                .orElse(List.of());
    }

    private List<PlayerMatchCalendarItem> findMatchesForPerson(
            UUID personId,
            LocalDateTime from,
            LocalDateTime to,
            List<TournamentStatus> statuses
    ) {
        return matchRepository.findScheduledCalendarMatchesForPlayer(personId, from, to, statuses).stream()
                .map(this::toPlayerMatchCalendarItem)
                .toList();
    }

    private List<TournamentEntity> findTournamentEntities(
            LocalDate from,
            LocalDate to,
            List<TournamentStatus> statuses,
            Surface surface,
            String location
    ) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(TournamentEntity.class);
        var tournament = query.from(TournamentEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(tournament.get("status").in(statuses));
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(tournament.get("playEndDate"), from));
        predicates.add(criteriaBuilder.lessThanOrEqualTo(tournament.get("playStartDate"), to));

        if (surface != null) {
            predicates.add(criteriaBuilder.equal(tournament.get("surface"), surface));
        }

        if (!isBlank(location)) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(tournament.get("location")),
                    "%" + location.toLowerCase(Locale.ROOT) + "%"
            ));
        }

        query.select(tournament)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(
                        criteriaBuilder.asc(tournament.get("playStartDate")),
                        criteriaBuilder.asc(tournament.get("startTime")),
                        criteriaBuilder.asc(tournament.get("formalName"))
                );

        return entityManager.createQuery(query).getResultList();
    }

    private TournamentCalendarItem toTournamentCalendarItem(TournamentEntity tournament) {
        return new TournamentCalendarItem(
                tournament.getId(),
                tournament.getFormalName(),
                tournament.getPlayStartDate(),
                tournament.getPlayEndDate(),
                tournament.getStartTime(),
                tournament.getLocation(),
                tournament.getSurface(),
                tournament.getMaxPlayers(),
                tournament.getStatus()
        );
    }

    private PlayerMatchCalendarItem toPlayerMatchCalendarItem(MatchEntity match) {
        EventEntity event = match.getDraw().getStage().getEvent();
        TournamentEntity tournament = event.getTournament();

        return new PlayerMatchCalendarItem(
                tournament.getId(),
                tournament.getFormalName(),
                event.getId(),
                resolveEventName(event),
                match.getId(),
                match.getRoundNumber(),
                match.getScheduledAt(),
                match.getScheduleTimeType(),
                match.getCourtResource() != null ? match.getCourtResource().getId() : null,
                match.getCourtResource() != null ? match.getCourtResource().getName() : match.getCourt(),
                match.getFirstInscription() != null ? match.getFirstInscription().getId() : null,
                resolveParticipantName(match.getFirstInscription()),
                match.getSecondInscription() != null ? match.getSecondInscription().getId() : null,
                resolveParticipantName(match.getSecondInscription()),
                match.getResult()
        );
    }

    private String resolveEventName(EventEntity event) {
        if (event == null) {
            return null;
        }

        if (!isBlank(event.getName())) {
            return event.getName();
        }

        String category = getCategoryLabel(event.getAgeCategory());
        String gender = toGenderLabel(event.getGender());
        if (category == null) {
            return gender;
        }
        if (gender == null) {
            return category;
        }

        return category + " - " + gender;
    }

    private String resolveParticipantName(InscriptionEntity inscription) {
        if (inscription == null || inscription.getParticipant() == null) {
            return "Bye";
        }

        ParticipantEntity participant = inscription.getParticipant();
        List<PersonEntity> people = getVisiblePeople(participant);
        if (people.isEmpty()) {
            return "Jugador pendiente";
        }

        return people.stream()
                .map(this::formatPersonName)
                .filter(name -> !isBlank(name))
                .toList()
                .stream()
                .reduce((left, right) -> left + " / " + right)
                .orElse("Jugador pendiente");
    }

    private List<PersonEntity> getVisiblePeople(ParticipantEntity participant) {
        if (participant.getIndividualPerson() == null && !isBlank(participant.getDisplayFirstName())) {
            return List.of(PersonEntity.builder()
                    .firstName(participant.getDisplayFirstName())
                    .lastName(participant.getDisplayLastName())
                    .build());
        }

        if (participant.getIndividualPerson() != null) {
            return List.of(participant.getIndividualPerson());
        }

        if (participant.getMembers() == null) {
            return List.of();
        }

        return participant.getMembers();
    }

    private String formatPersonName(PersonEntity person) {
        String firstName = person.getFirstName() != null ? person.getFirstName().trim() : "";
        String lastName = person.getLastName() != null ? person.getLastName().trim() : "";
        return (firstName + " " + lastName).trim();
    }

    private String getCategoryLabel(RefAgeCategoryEntity ageCategory) {
        return ageCategory != null ? ageCategory.getCategory() : null;
    }

    private String toGenderLabel(String gender) {
        return switch (normalizeGender(gender)) {
            case "MALE" -> "Masculino";
            case "FEMALE" -> "Femenino";
            case "MIXED" -> "Mixto";
            default -> null;
        };
    }

    private String normalizeGender(String gender) {
        if (isBlank(gender)) {
            return "UNKNOWN";
        }

        return gender.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
