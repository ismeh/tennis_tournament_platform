package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Category;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findByName(String name);
    Category save(Category category);
}
