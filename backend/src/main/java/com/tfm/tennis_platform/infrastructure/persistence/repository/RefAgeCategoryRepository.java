package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefAgeCategoryRepository extends JpaRepository<RefAgeCategoryEntity, Integer> {
}

