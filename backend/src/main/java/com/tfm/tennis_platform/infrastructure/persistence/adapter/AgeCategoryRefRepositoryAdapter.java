package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.domain.port.out.AgeCategoryRefRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.AgeCategoryRefMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.RefAgeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AgeCategoryRefRepositoryAdapter implements AgeCategoryRefRepository {

    private final RefAgeCategoryRepository repository;
    private final AgeCategoryRefMapper mapper;

    @Override
    public List<AgeCategoryRef> findAll() {
        return repository.findAllOrdered().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AgeCategoryRef> findByOrganizerId(UUID organizerId) {
        return repository.findByOrganizerIdOrdered(organizerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AgeCategoryRef> findById(Integer id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public AgeCategoryRef save(AgeCategoryRef category) {
        RefAgeCategoryEntity entity = mapper.toEntity(category);
        RefAgeCategoryEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByOrganizerIdAndCategory(UUID organizerId, String category) {
        return repository.existsByOrganizerIdAndCategoryIgnoreCase(organizerId, category);
    }
}
