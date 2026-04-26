package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MatchMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MatchRepositoryAdapter implements MatchRepository {

    private final JpaMatchRepository matchRepository;
    private final MatchMapper mapper;

    @Override
    public Match save(Match match) {
        return mapper.toDomain(matchRepository.save(mapper.toEntity(match)));
    }

    @Override
    public List<Match> findByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Match> findById(String id) {
        return matchRepository.findById(id).map(mapper::toDomain);
    }
}
