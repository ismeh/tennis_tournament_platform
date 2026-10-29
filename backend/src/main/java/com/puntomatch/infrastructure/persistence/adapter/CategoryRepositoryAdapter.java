package com.puntomatch.infrastructure.persistence.adapter;

import com.puntomatch.domain.port.out.CategoryRepository;
import com.puntomatch.domain.models.Category;
import com.puntomatch.infrastructure.persistence.mapper.CategoryMapper;
import com.puntomatch.infrastructure.persistence.repository.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final JpaCategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    @Override
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        return mapper.toDomain(categoryRepository.save(mapper.toEntity(category)));
    }
}
