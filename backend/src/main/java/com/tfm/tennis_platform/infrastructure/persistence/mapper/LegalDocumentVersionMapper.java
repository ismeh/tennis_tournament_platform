package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.infrastructure.persistence.entity.LegalDocumentVersionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LegalDocumentVersionMapper {

    LegalDocumentVersion toDomain(LegalDocumentVersionEntity entity);

    LegalDocumentVersionEntity toEntity(LegalDocumentVersion domain);
}
