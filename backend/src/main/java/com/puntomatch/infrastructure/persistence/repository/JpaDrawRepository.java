package com.puntomatch.infrastructure.persistence.repository;

import com.puntomatch.infrastructure.persistence.entity.DrawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaDrawRepository extends JpaRepository<DrawEntity, UUID> {
    List<DrawEntity> findByStage_Id(UUID stageId);
}