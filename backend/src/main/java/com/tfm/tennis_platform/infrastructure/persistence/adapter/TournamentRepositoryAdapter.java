package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MatchDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TournamentRepositoryAdapter implements TournamentRepository {

    private final JpaTournamentRepository tournamentJpaRepository;
    private final TournamentEntityMapper mapper;
    private final MatchDomainMapper matchDomainMapper;
    private final JpaMatchRepository matchJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Tournament save(Tournament tournament) {
        TournamentEntity tournamentEntity = tournamentJpaRepository.findById(tournament.getId())
                .orElseGet(() -> tournamentJpaRepository.save(mapper.toEntity(tournament)));
        mapper.updateEntityFromDomain(tournament, tournamentEntity);

        Map<UUID, EventEntity> existingEventsById = tournamentEntity.getEvents().stream()
            .collect(Collectors.toMap(EventEntity::getId, event -> event, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> incomingEventIds = tournament.getEvents().stream()
            .map(domainEvent -> domainEvent.getId())
            .collect(Collectors.toSet());

        tournamentEntity.getEvents().removeIf(eventEntity -> !incomingEventIds.contains(eventEntity.getId()));

        for (var domainEvent : tournament.getEvents()) {
            RefAgeCategoryEntity categoryProxy = entityManager.getReference(
                RefAgeCategoryEntity.class,
                domainEvent.getCategoryId()
            );

            EventEntity existingEvent = existingEventsById.get(domainEvent.getId());

            if (existingEvent != null) {
                existingEvent.setGender(domainEvent.getGender());
                existingEvent.setAgeCategory(categoryProxy);
                existingEvent.setTournament(tournamentEntity);
                updateEventStages(existingEvent, domainEvent);
                continue;
            }

            EventEntity newEvent = EventEntity.builder()
                .id(domainEvent.getId())
                .gender(domainEvent.getGender())
                .ageCategory(categoryProxy)
                .tournament(tournamentEntity)
                .build();
            updateEventStages(newEvent, domainEvent);
            tournamentEntity.getEvents().add(newEvent);
        }

        tournamentJpaRepository.flush();

        return enrichMatches(mapper.toDomain(tournamentEntity), tournamentEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tournament> findAll() {
        return tournamentJpaRepository.findAll().stream()
                .map(entity -> enrichMatches(mapper.toDomain(entity), entity))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TournamentSummary> findSummaries() {
        List<TournamentSummary> summaries = findBaseSummaries();
        Map<UUID, Boolean> professionalFlags = findProfessionalTournamentFlags(summaries.stream()
                .map(TournamentSummary::id)
                .toList());

        return summaries.stream()
                .map(summary -> withProfessionalFlag(summary, professionalFlags.getOrDefault(summary.id(), false)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tournament> findById(UUID id) {
        return findDetailedById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProfessionalTournament(UUID id) {
        TypedQuery<Object[]> query = entityManager.createQuery("""
                select count(i.id),
                       sum(case when p.participantSource = :professionalSource then 1 else 0 end)
                from InscriptionEntity i
                join i.event e
                join i.participant p
                where e.tournament.id = :tournamentId
                """, Object[].class);
        query.setParameter("professionalSource", ParticipantSource.PROFESSIONAL);
        query.setParameter("tournamentId", id);

        Object[] row = query.getSingleResult();
        return toProfessionalFlag((Number) row[0], (Number) row[1]);
    }

    private List<TournamentSummary> findBaseSummaries() {
        return entityManager.createQuery("""
                select new com.tfm.tennis_platform.domain.models.TournamentSummary(
                    t.id,
                    t.formalName,
                    t.playStartDate,
                    t.playEndDate,
                    t.startTime,
                    t.inscriptionStartDate,
                    t.inscriptionEndDate,
                    t.surface,
                    t.maxPlayers,
                    t.location,
                    t.status,
                    false
                )
                from TournamentEntity t
                order by t.playStartDate asc, t.startTime asc, t.formalName asc
                """, TournamentSummary.class)
                .getResultList();
    }

    private Map<UUID, Boolean> findProfessionalTournamentFlags(List<UUID> tournamentIds) {
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }

        TypedQuery<Object[]> query = entityManager.createQuery("""
                select e.tournament.id,
                       count(i.id),
                       sum(case when p.participantSource = :professionalSource then 1 else 0 end)
                from InscriptionEntity i
                join i.event e
                join i.participant p
                where e.tournament.id in :tournamentIds
                group by e.tournament.id
                """, Object[].class);
        query.setParameter("professionalSource", ParticipantSource.PROFESSIONAL);
        query.setParameter("tournamentIds", tournamentIds);

        return query.getResultList().stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> toProfessionalFlag((Number) row[1], (Number) row[2]),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private boolean toProfessionalFlag(Number totalInscriptions, Number professionalInscriptions) {
        long total = totalInscriptionsToLong(totalInscriptions);
        long professional = totalInscriptionsToLong(professionalInscriptions);
        return total > 0 && total == professional;
    }

    private long totalInscriptionsToLong(Number value) {
        return value != null ? value.longValue() : 0L;
    }

    private TournamentSummary withProfessionalFlag(TournamentSummary summary, boolean professionalTournament) {
        return new TournamentSummary(
                summary.id(),
                summary.name(),
                summary.playStartDate(),
                summary.playEndDate(),
                summary.startTime(),
                summary.inscriptionStartDate(),
                summary.inscriptionEndDate(),
                summary.surface(),
                summary.maxPlayers(),
                summary.location(),
                summary.status(),
                professionalTournament
        );
    }

    private Optional<Tournament> findDetailedById(UUID id) {
        List<Object[]> tournamentRows = entityManager.createQuery("""
                select t.id,
                       t.formalName,
                       t.playStartDate,
                       t.playEndDate,
                       t.startTime,
                       t.inscriptionStartDate,
                       t.inscriptionEndDate,
                       t.surface,
                       t.maxPlayers,
                       t.location,
                       t.status,
                       createdBy.id
                from TournamentEntity t
                left join t.createdBy createdBy
                where t.id = :tournamentId
                """, Object[].class)
                .setParameter("tournamentId", id)
                .getResultList();

        if (tournamentRows.isEmpty()) {
            return Optional.empty();
        }

        List<Event> events = findDetailedEvents(id);
        Object[] tournament = tournamentRows.getFirst();

        return Optional.of(Tournament.builder()
                .id((UUID) tournament[0])
                .name((String) tournament[1])
                .playPeriod(new TournamentPeriod(
                        (java.time.LocalDate) tournament[2],
                        (java.time.LocalDate) tournament[3]
                ))
                .startTime((java.time.LocalTime) tournament[4])
                .inscriptionPeriod(new TournamentPeriod(
                        (java.time.LocalDate) tournament[5],
                        (java.time.LocalDate) tournament[6]
                ))
                .surface((com.tfm.tennis_platform.domain.models.enums.Surface) tournament[7])
                .maxPlayers((Integer) tournament[8])
                .location((String) tournament[9])
                .state((com.tfm.tennis_platform.domain.models.enums.TournamentStatus) tournament[10])
                .createdBy(tournament[11] != null ? Member.builder().id((UUID) tournament[11]).build() : null)
                .events(events)
                .build());
    }

    private List<Event> findDetailedEvents(UUID tournamentId) {
        List<Object[]> eventRows = entityManager.createQuery("""
                select e.id,
                       e.ageCategory.id,
                       e.gender
                from EventEntity e
                where e.tournament.id = :tournamentId
                order by e.ageCategory.id asc, e.gender asc, e.id asc
                """, Object[].class)
                .setParameter("tournamentId", tournamentId)
                .getResultList();

        if (eventRows.isEmpty()) {
            return List.of();
        }

        List<UUID> eventIds = eventRows.stream()
                .map(row -> (UUID) row[0])
                .toList();
        Map<UUID, List<Stage>> stagesByEventId = findDetailedStages(tournamentId, eventIds);

        return eventRows.stream()
                .map(row -> Event.builder()
                        .id((UUID) row[0])
                        .tournamentId(tournamentId)
                        .categoryId((Integer) row[1])
                        .gender((String) row[2])
                        .stages(stagesByEventId.getOrDefault((UUID) row[0], List.of()))
                        .build())
                .toList();
    }

    private Map<UUID, List<Stage>> findDetailedStages(UUID tournamentId, List<UUID> eventIds) {
        List<Object[]> stageRows = entityManager.createQuery("""
                select s.id,
                       s.event.id,
                       s.order,
                       s.stageType,
                       s.description
                from StageEntity s
                where s.event.id in :eventIds
                order by s.event.id asc, s.order asc, s.id asc
                """, Object[].class)
                .setParameter("eventIds", eventIds)
                .getResultList();

        if (stageRows.isEmpty()) {
            return Map.of();
        }

        List<UUID> stageIds = stageRows.stream()
                .map(row -> (UUID) row[0])
                .toList();
        Map<UUID, List<Draw>> drawsByStageId = findDetailedDraws(tournamentId, stageIds);

        return stageRows.stream()
                .map(row -> Stage.builder()
                        .id((UUID) row[0])
                        .eventId((UUID) row[1])
                        .stageNumber((Integer) row[2])
                        .stageType(row[3] != null ? StageType.valueOf((String) row[3]) : null)
                        .description((String) row[4])
                        .draws(drawsByStageId.getOrDefault((UUID) row[0], List.of()))
                        .build())
                .collect(Collectors.groupingBy(
                        Stage::getEventId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<UUID, List<Draw>> findDetailedDraws(UUID tournamentId, List<UUID> stageIds) {
        List<Object[]> drawRows = entityManager.createQuery("""
                select d.id,
                       d.stage.id,
                       d.drawType,
                       d.label
                from DrawEntity d
                where d.stage.id in :stageIds
                order by d.stage.id asc, d.label asc, d.id asc
                """, Object[].class)
                .setParameter("stageIds", stageIds)
                .getResultList();

        if (drawRows.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<Match>> matchesByDrawId = findDetailedMatchesByDrawId(tournamentId);

        return drawRows.stream()
                .map(row -> Draw.builder()
                        .id((UUID) row[0])
                        .stageId((UUID) row[1])
                        .drawType(row[2] != null ? DrawType.valueOf((String) row[2]) : null)
                        .drawName((String) row[3])
                        .label((String) row[3])
                        .matches(matchesByDrawId.getOrDefault((UUID) row[0], List.of()))
                        .build())
                .collect(Collectors.groupingBy(
                        Draw::getStageId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<UUID, List<Match>> findDetailedMatchesByDrawId(UUID tournamentId) {
        return matchDomainMapper.toDomainList(matchJpaRepository.findByTournamentId(tournamentId)).stream()
                .collect(Collectors.groupingBy(
                        Match::getDrawId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Tournament enrichMatches(Tournament tournament, TournamentEntity entity) {
        if (tournament == null || tournament.getEvents() == null || entity == null || entity.getEvents() == null) {
            return tournament;
        }

        List<MatchEntity> matchEntities = entity.getEvents().stream()
                .filter(event -> event.getStages() != null)
                .flatMap(event -> event.getStages().stream())
                .filter(stage -> stage.getDraws() != null)
                .flatMap(stage -> stage.getDraws().stream())
                .filter(draw -> draw.getMatches() != null)
                .flatMap(draw -> draw.getMatches().stream())
                .toList();

        Map<UUID, Match> matchesById = matchDomainMapper.toDomainList(matchEntities).stream()
                .collect(Collectors.toMap(Match::getId, match -> match, (left, right) -> left, LinkedHashMap::new));

        tournament.getEvents().stream()
                .filter(event -> event.getStages() != null)
                .flatMap(event -> event.getStages().stream())
                .filter(stage -> stage.getDraws() != null)
                .flatMap(stage -> stage.getDraws().stream())
                .filter(draw -> draw.getMatches() != null)
                .forEach(draw -> draw.setMatches(draw.getMatches().stream()
                        .map(match -> matchesById.getOrDefault(match.getId(), match))
                        .toList()));

        return tournament;
    }

    private void updateEventStages(EventEntity eventEntity, Event domainEvent) {
        Map<UUID, StageEntity> existingStagesById = eventEntity.getStages().stream()
            .collect(Collectors.toMap(StageEntity::getId, stage -> stage, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> incomingStageIds = domainEvent.getStages().stream()
            .map(domainStage -> domainStage.getId())
            .collect(Collectors.toSet());

        eventEntity.getStages().removeIf(stageEntity -> !incomingStageIds.contains(stageEntity.getId()));

        for (var domainStage : domainEvent.getStages()) {
            StageEntity existingStage = existingStagesById.get(domainStage.getId());

            if (existingStage != null) {
                existingStage.setOrder(domainStage.getStageNumber());
                existingStage.setStageType(domainStage.getStageType() != null ? domainStage.getStageType().name() : null);
                existingStage.setDescription(domainStage.getDescription());
                updateStageDraws(existingStage, domainStage);
                continue;
            }

            StageEntity newStage = StageEntity.builder()
                .id(domainStage.getId())
                .event(eventEntity)
                .order(domainStage.getStageNumber())
                .stageType(domainStage.getStageType() != null ? domainStage.getStageType().name() : null)
                .description(domainStage.getDescription())
                .build();
            updateStageDraws(newStage, domainStage);
            eventEntity.getStages().add(newStage);
        }
    }

    private void updateStageDraws(StageEntity stageEntity, Stage domainStage) {
        if (domainStage.getDraws() == null || domainStage.getDraws().isEmpty()) {
            return;
        }

        Map<UUID, DrawEntity> existingDrawsById = stageEntity.getDraws().stream()
            .collect(Collectors.toMap(DrawEntity::getId, draw -> draw, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> incomingDrawIds = domainStage.getDraws().stream()
            .map(Draw::getId)
            .collect(Collectors.toSet());

        stageEntity.getDraws().removeIf(drawEntity -> !incomingDrawIds.contains(drawEntity.getId()));

        for (var domainDraw : domainStage.getDraws()) {
            DrawEntity existingDraw = existingDrawsById.get(domainDraw.getId());

            if (existingDraw != null) {
                existingDraw.setDrawType(domainDraw.getDrawType() != null ? domainDraw.getDrawType().name() : null);
                existingDraw.setLabel(domainDraw.getLabel() != null ? domainDraw.getLabel() : domainDraw.getDrawName());
                existingDraw.setStage(stageEntity);
                continue;
            }

            DrawEntity newDraw = DrawEntity.builder()
                .id(domainDraw.getId())
                .stage(stageEntity)
                .drawType(domainDraw.getDrawType() != null ? domainDraw.getDrawType().name() : null)
                .label(domainDraw.getLabel() != null ? domainDraw.getLabel() : domainDraw.getDrawName())
                .build();
            stageEntity.getDraws().add(newDraw);
        }
    }
}
