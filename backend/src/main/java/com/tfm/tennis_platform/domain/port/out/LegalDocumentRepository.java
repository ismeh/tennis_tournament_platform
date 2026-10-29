package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;

import java.util.Optional;
import java.util.UUID;

public interface LegalDocumentRepository {
    LegalDocumentVersion save(LegalDocumentVersion document);
    Optional<LegalDocumentVersion> findLatestByDocumentType(DocumentType documentType);
    Optional<LegalDocumentVersion> findByDocumentTypeAndVersion(DocumentType documentType, String version);
}
