package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TournamentRepositoryAdapter implements TournamentRepository {

    private final JpaTournamentRepository tournamentRepository;
    private final TournamentMapper mapper;

    @Override
    public Tournament save(Tournament tournament) {
        return mapper.toDomain(tournamentRepository.save(mapper.toEntity(tournament)));
    }

    @Override
    public List<Tournament> findAll() {
        return tournamentRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id).map(mapper::toDomain);
    }
}
