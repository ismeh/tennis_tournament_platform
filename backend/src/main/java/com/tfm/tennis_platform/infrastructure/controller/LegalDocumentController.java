package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.LegalDocumentService;
import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.infrastructure.controller.dto.LegalDocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping("/{type}/current")
    public ResponseEntity<LegalDocumentResponse> getCurrentDocument(@PathVariable String type) {
        DocumentType documentType = parseDocumentType(type);
        LegalDocumentVersion version = legalDocumentService.getCurrentVersion(documentType);
        return ResponseEntity.ok(toResponse(version));
    }

    @GetMapping("/{type}/{version}")
    public ResponseEntity<LegalDocumentResponse> getDocumentByVersion(
            @PathVariable String type,
            @PathVariable String version) {
        DocumentType documentType = parseDocumentType(type);
        LegalDocumentVersion docVersion = legalDocumentService.getVersion(documentType, version);
        return ResponseEntity.ok(toResponse(docVersion));
    }

    private DocumentType parseDocumentType(String type) {
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de documento no válido: " + type);
        }
    }

    private LegalDocumentResponse toResponse(LegalDocumentVersion version) {
        return new LegalDocumentResponse(
                version.getDocumentType().name(),
                version.getVersion(),
                version.getContentSnapshot(),
                version.getCreatedAt()
        );
    }
}
