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
