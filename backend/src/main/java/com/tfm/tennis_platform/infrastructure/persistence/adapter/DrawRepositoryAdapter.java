package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.port.out.DrawRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.DrawDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaDrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DrawRepositoryAdapter implements DrawRepository {

    private final JpaDrawRepository drawRepository;
    private final DrawDomainMapper mapper;

    @Override
    public Draw save(Draw draw) {
        return mapper.toDomain(drawRepository.save(mapper.toEntity(draw)));
    }

    @Override
    public Optional<Draw> findById(UUID id) {
        return drawRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Draw> findByStageId(UUID stageId) {
        return drawRepository.findByStage_Id(stageId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}