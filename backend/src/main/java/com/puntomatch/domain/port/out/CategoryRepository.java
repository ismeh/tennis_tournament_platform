package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Category;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findByName(String name);
    Category save(Category category);
}
