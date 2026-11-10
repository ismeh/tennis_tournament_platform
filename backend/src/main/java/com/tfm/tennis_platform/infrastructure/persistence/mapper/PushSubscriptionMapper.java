package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PushSubscriptionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PushSubscriptionMapper {

    PushSubscription toDomain(PushSubscriptionEntity entity);

    PushSubscriptionEntity toEntity(PushSubscription domain);
}
