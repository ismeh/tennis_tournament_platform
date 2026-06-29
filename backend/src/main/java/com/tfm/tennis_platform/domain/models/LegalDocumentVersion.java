package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LegalDocumentVersion {
    private final Long id;
    private final DocumentType documentType;
    private final String version;
    private final String contentSnapshot;
    private final LocalDateTime createdAt;
}
