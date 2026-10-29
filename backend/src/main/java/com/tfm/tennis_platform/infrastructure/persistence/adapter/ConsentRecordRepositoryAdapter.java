package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.port.out.ConsentRecordRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.LegalDocumentVersionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ConsentRecordMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaConsentRecordRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaLegalDocumentVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConsentRecordRepositoryAdapter implements ConsentRecordRepository {

    private final JpaConsentRecordRepository jpaRepository;
    private final JpaLegalDocumentVersionRepository legalDocumentRepository;
    private final ConsentRecordMapper mapper;

    @Override
    public ConsentRecord save(ConsentRecord record) {
        LegalDocumentVersionEntity documentVersion = legalDocumentRepository
                .findById(record.getDocumentVersionId())
                .orElseThrow(() -> new EntityNotFoundException("Legal document version not found: " + record.getDocumentVersionId()));

        var entity = mapper.toEntity(record);
        entity.setDocumentVersion(documentVersion);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ConsentRecord> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ConsentRecord> findLatestByUserIdAndDocumentType(UUID userId, DocumentType documentType) {
        return jpaRepository.findLatestByUserIdAndDocumentType(userId, documentType)
                .map(mapper::toDomain);
    }
}
