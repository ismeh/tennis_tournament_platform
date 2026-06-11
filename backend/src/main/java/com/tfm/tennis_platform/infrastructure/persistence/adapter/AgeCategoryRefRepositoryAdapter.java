package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.domain.port.out.AgeCategoryRefRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.AgeCategoryRefMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.RefAgeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgeCategoryRefRepositoryAdapter implements AgeCategoryRefRepository {

    private final RefAgeCategoryRepository repository;
    private final AgeCategoryRefMapper mapper;

    @Override
    public List<AgeCategoryRef> findAll() {
        return repository.findAllOrdered().stream().map(mapper::toDomain).toList();
    }
}
