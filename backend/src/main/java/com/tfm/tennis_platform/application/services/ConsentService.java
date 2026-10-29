package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.domain.models.enums.ConsentAction;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.port.out.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentRecordRepository consentRecordRepository;

    @Transactional
    public void recordConsent(UUID userId, DocumentType documentType, ConsentAction action,
                              Long documentVersionId) {
        ConsentRecord record = ConsentRecord.builder()
                .userId(userId)
                .documentType(documentType)
                .action(action)
                .documentVersionId(documentVersionId)
                .build();

        consentRecordRepository.save(record);
        log.info("Consent recorded: user={}, type={}, action={}, version={}",
                userId, documentType, action, documentVersionId);
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> getConsentHistory(UUID userId) {
        return consentRecordRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<ConsentRecord> getCurrentConsent(UUID userId, DocumentType documentType) {
        return consentRecordRepository.findLatestByUserIdAndDocumentType(userId, documentType);
    }
}
