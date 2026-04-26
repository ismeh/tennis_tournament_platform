package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
            continue;
            }

            EventEntity newEvent = EventEntity.builder()
                .id(domainEvent.getId())
                .gender(domainEvent.getGender())
                .ageCategory(categoryProxy)
                .tournament(tournamentEntity)
                .build();
            tournamentEntity.getEvents().add(newEvent);
        }

        return mapper.toDomain(tournamentEntity);
    }

    @Override
    public List<Tournament> findAll() {
        return tournamentJpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Tournament> findById(UUID id) {
        return tournamentJpaRepository.findById(id).map(mapper::toDomain);
    }
}
