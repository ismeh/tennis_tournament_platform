package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.CourtDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourtRepositoryAdapter implements CourtRepository {

    private final JpaCourtRepository courtRepository;
    private final CourtDomainMapper mapper;

    @Override
    public Court save(Court court) {
        return mapper.toDomain(courtRepository.save(mapper.toEntity(court)));
    }

    @Override
    public List<Court> findByTournamentId(UUID tournamentId) {
        return courtRepository.findByTournamentIdOrderByNameAsc(tournamentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Court> findByIdAndTournamentId(UUID id, UUID tournamentId) {
        return courtRepository.findByIdAndTournamentId(id, tournamentId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTournamentIdAndName(UUID tournamentId, String name) {
        return courtRepository.existsByTournamentIdAndNameIgnoreCase(tournamentId, name);
    }

    @Override
    public void deleteById(UUID id) {
        courtRepository.deleteById(id);
    }
}
