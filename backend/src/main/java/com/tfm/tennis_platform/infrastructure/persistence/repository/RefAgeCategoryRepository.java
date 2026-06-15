package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RefAgeCategoryRepository extends JpaRepository<RefAgeCategoryEntity, Integer> {
    @Query("SELECT category FROM RefAgeCategoryEntity category ORDER BY COALESCE(category.displayOrder, category.id), category.id")
    List<RefAgeCategoryEntity> findAllOrdered();

    @Query("SELECT category FROM RefAgeCategoryEntity category WHERE category.organizerId = :organizerId ORDER BY category.category")
    List<RefAgeCategoryEntity> findByOrganizerIdOrdered(@Param("organizerId") UUID organizerId);

    boolean existsByOrganizerIdAndCategoryIgnoreCase(UUID organizerId, String category);
}
