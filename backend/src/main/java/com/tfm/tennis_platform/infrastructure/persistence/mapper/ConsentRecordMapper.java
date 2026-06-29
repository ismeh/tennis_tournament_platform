package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ConsentRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConsentRecordMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "documentVersionId", source = "documentVersion.id")
    ConsentRecord toDomain(ConsentRecordEntity entity);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "documentVersion", ignore = true)
    ConsentRecordEntity toEntity(ConsentRecord domain);
}
