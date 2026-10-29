package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.port.out.StageRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.StageDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StageRepositoryAdapter implements StageRepository {

    private final JpaStageRepository stageRepository;
    private final StageDomainMapper mapper;

    @Override
    public Stage save(Stage stage) {
        return mapper.toDomain(stageRepository.save(mapper.toEntity(stage)));
    }

    @Override
    public Optional<Stage> findById(UUID id) {
        return stageRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Stage> findByEventId(UUID eventId) {
        return stageRepository.findByEvent_Id(eventId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}