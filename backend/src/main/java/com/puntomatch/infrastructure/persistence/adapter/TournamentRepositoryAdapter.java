package com.puntomatch.infrastructure.persistence.adapter;

import com.puntomatch.domain.port.out.TournamentRepository;
import com.puntomatch.domain.models.Tournament;
import com.puntomatch.infrastructure.persistence.entity.EventEntity;
import com.puntomatch.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.puntomatch.infrastructure.persistence.entity.TournamentEntity;
import com.puntomatch.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.puntomatch.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        List<EventEntity> eventEntities = tournament.getEvents().stream()
                .map(domainEvent -> {
                    RefAgeCategoryEntity categoryProxy = entityManager.getReference(
                            RefAgeCategoryEntity.class,
                            domainEvent.getCategoryId()
                    );

                    return EventEntity.builder()
                            .id(domainEvent.getId())
                            .gender(domainEvent.getGender())
                            .ageCategory(categoryProxy)
                            .tournament(tournamentEntity)
                            .build();
                }).toList();

        tournamentEntity.getEvents().clear();
        tournamentEntity.getEvents().addAll(eventEntities);

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
