package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.port.out.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;

    @Transactional(readOnly = true)
    public LegalDocumentVersion getCurrentVersion(DocumentType documentType) {
        return legalDocumentRepository.findLatestByDocumentType(documentType)
                .orElseThrow(() -> new InvalidArgumentException(
                        "No existe una versión activa para el documento: " + documentType));
    }

    @Transactional(readOnly = true)
    public LegalDocumentVersion getVersion(DocumentType documentType, String version) {
        return legalDocumentRepository.findByDocumentTypeAndVersion(documentType, version)
                .orElseThrow(() -> new InvalidArgumentException(
                        "No existe la versión " + version + " para el documento: " + documentType));
    }

    @Transactional
    public LegalDocumentVersion createVersion(DocumentType documentType, String version, String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidArgumentException("El contenido del documento no puede estar vacío.");
        }
        if (version == null || version.isBlank()) {
            throw new InvalidArgumentException("La versión del documento es obligatoria.");
        }

        legalDocumentRepository.findByDocumentTypeAndVersion(documentType, version)
                .ifPresent(existing -> {
                    throw new InvalidArgumentException(
                            "Ya existe la versión " + version + " para el documento: " + documentType);
                });

        LegalDocumentVersion document = LegalDocumentVersion.builder()
                .documentType(documentType)
                .version(version)
                .contentSnapshot(content)
                .build();

        LegalDocumentVersion saved = legalDocumentRepository.save(document);
        log.info("Created legal document version: {} {}", documentType, version);
        return saved;
    }
}
