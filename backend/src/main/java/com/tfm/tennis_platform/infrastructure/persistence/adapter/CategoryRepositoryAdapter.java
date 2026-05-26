package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.CategoryRepository;
import com.tfm.tennis_platform.domain.models.Category;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.CategoryMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCategoryRepository;
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
