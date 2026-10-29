package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ScheduleConfigDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaScheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleConfigRepositoryAdapter implements ScheduleConfigRepository {

    private final JpaScheduleConfigRepository jpaRepository;
    private final ScheduleConfigDomainMapper mapper;

    @Override
    public Optional<ScheduleConfig> findByTournamentId(UUID tournamentId) {
        return jpaRepository.findByTournamentId(tournamentId).map(mapper::toDomain);
    }

    @Override
    public ScheduleConfig save(ScheduleConfig scheduleConfig) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(scheduleConfig)));
    }

    @Override
    public void deleteByTournamentId(UUID tournamentId) {
        jpaRepository.deleteByTournamentId(tournamentId);
    }
}
