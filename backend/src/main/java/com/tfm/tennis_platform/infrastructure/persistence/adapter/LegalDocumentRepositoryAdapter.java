package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.port.out.LegalDocumentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.LegalDocumentVersionMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaLegalDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LegalDocumentRepositoryAdapter implements LegalDocumentRepository {

    private final JpaLegalDocumentVersionRepository jpaRepository;
    private final LegalDocumentVersionMapper mapper;

    @Override
    public LegalDocumentVersion save(LegalDocumentVersion document) {
        var entity = mapper.toEntity(document);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<LegalDocumentVersion> findLatestByDocumentType(DocumentType documentType) {
        return jpaRepository.findLatestByDocumentType(documentType)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<LegalDocumentVersion> findByDocumentTypeAndVersion(DocumentType documentType, String version) {
        return jpaRepository.findByDocumentTypeAndVersion(documentType, version)
                .map(mapper::toDomain);
    }
}
