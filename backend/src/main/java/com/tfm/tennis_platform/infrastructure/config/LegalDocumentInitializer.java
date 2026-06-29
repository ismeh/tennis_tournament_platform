package com.tfm.tennis_platform.infrastructure.config;

import com.tfm.tennis_platform.application.services.LegalDocumentService;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.port.out.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentInitializer implements ApplicationRunner {

    private final LegalDocumentService legalDocumentService;
    private final LegalDocumentRepository legalDocumentRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (legalDocumentRepository.findLatestByDocumentType(DocumentType.PRIVACY_POLICY).isEmpty()) {
                log.info("Seeding default PRIVACY_POLICY version v1.0...");
                legalDocumentService.createVersion(
                        DocumentType.PRIVACY_POLICY,
                        "v1.0",
                        "Esta es la Política de Privacidad de PuntoMatch."
                );
            }
            if (legalDocumentRepository.findLatestByDocumentType(DocumentType.TERMS_CONDITIONS).isEmpty()) {
                log.info("Seeding default TERMS_CONDITIONS version v1.0...");
                legalDocumentService.createVersion(
                        DocumentType.TERMS_CONDITIONS,
                        "v1.0",
                        "Estos son los Términos y Condiciones de PuntoMatch."
                );
            }
        } catch (Exception e) {
            log.error("Error seeding default legal documents", e);
        }
    }
}
