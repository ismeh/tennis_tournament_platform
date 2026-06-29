package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.LegalDocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaLegalDocumentVersionRepository extends JpaRepository<LegalDocumentVersionEntity, Long> {

    @Query("SELECT ldv FROM LegalDocumentVersionEntity ldv WHERE ldv.documentType = :type ORDER BY ldv.createdAt DESC LIMIT 1")
    Optional<LegalDocumentVersionEntity> findLatestByDocumentType(@Param("type") DocumentType documentType);

    Optional<LegalDocumentVersionEntity> findByDocumentTypeAndVersion(DocumentType documentType, String version);
}
