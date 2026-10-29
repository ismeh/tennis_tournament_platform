package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.ConsentAction;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsentRecord {
    private final UUID id;
    private final UUID userId;
    private final DocumentType documentType;
    private final ConsentAction action;
    private final Long documentVersionId;
    private final LocalDateTime createdAt;
}
