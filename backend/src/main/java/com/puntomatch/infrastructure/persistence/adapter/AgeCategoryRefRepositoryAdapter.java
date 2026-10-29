package com.puntomatch.infrastructure.persistence.adapter;

import com.puntomatch.domain.models.AgeCategoryRef;
import com.puntomatch.domain.port.out.AgeCategoryRefRepository;
import com.puntomatch.infrastructure.persistence.mapper.AgeCategoryRefMapper;
import com.puntomatch.infrastructure.persistence.repository.RefAgeCategoryRepository;
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
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
