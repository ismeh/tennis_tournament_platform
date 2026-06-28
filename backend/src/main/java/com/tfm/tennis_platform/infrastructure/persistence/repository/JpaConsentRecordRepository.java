package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ConsentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaConsentRecordRepository extends JpaRepository<ConsentRecordEntity, UUID> {

    @Query("SELECT cr FROM ConsentRecordEntity cr WHERE cr.userId = :userId ORDER BY cr.createdAt DESC")
    List<ConsentRecordEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT cr FROM ConsentRecordEntity cr WHERE cr.userId = :userId AND cr.documentType = :type ORDER BY cr.createdAt DESC LIMIT 1")
    Optional<ConsentRecordEntity> findLatestByUserIdAndDocumentType(@Param("userId") UUID userId, @Param("type") DocumentType documentType);
}
