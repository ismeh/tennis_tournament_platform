package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.domain.port.out.TournamentUmpireRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentUmpireMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentUmpireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TournamentUmpireRepositoryAdapter implements TournamentUmpireRepository {

    private final JpaTournamentUmpireRepository jpaRepository;
    private final TournamentUmpireMapper mapper;

    @Override
    public TournamentUmpire save(TournamentUmpire tournamentUmpire) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(tournamentUmpire)));
    }

    @Override
    public List<TournamentUmpire> findByTournamentId(UUID tournamentId) {
        return jpaRepository.findByTournamentIdOrderByAssignedAtAsc(tournamentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TournamentUmpire> findByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId) {
        return jpaRepository.findByTournamentIdAndUmpireId(tournamentId, umpireId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId) {
        return jpaRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId);
    }

    @Override
    public void deleteByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId) {
        jpaRepository.deleteByTournamentIdAndUmpireId(tournamentId, umpireId);
    }
}
