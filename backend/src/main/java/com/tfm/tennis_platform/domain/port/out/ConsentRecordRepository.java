package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository {
    ConsentRecord save(ConsentRecord record);
    List<ConsentRecord> findByUserId(UUID userId);
    Optional<ConsentRecord> findLatestByUserIdAndDocumentType(UUID userId, DocumentType documentType);
}
