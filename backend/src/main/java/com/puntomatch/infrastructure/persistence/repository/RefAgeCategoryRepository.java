package com.puntomatch.infrastructure.persistence.repository;

import com.puntomatch.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefAgeCategoryRepository extends JpaRepository<RefAgeCategoryEntity, Integer> {
}

