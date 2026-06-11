package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RefAgeCategoryRepository extends JpaRepository<RefAgeCategoryEntity, Integer> {
    @Query("SELECT category FROM RefAgeCategoryEntity category ORDER BY COALESCE(category.displayOrder, category.id), category.id")
    List<RefAgeCategoryEntity> findAllOrdered();
}
