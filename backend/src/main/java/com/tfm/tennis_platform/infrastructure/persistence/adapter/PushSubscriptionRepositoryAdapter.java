package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.domain.port.out.PushSubscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.PushSubscriptionMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PushSubscriptionRepositoryAdapter implements PushSubscriptionRepository {

    private final JpaPushSubscriptionRepository repository;
    private final PushSubscriptionMapper mapper;

    @Override
    public PushSubscription save(PushSubscription subscription) {
        return mapper.toDomain(repository.save(mapper.toEntity(subscription)));
    }

    @Override
    public Optional<PushSubscription> findByEndpoint(String endpoint) {
        return repository.findByEndpoint(endpoint).map(mapper::toDomain);
    }

    @Override
    public List<PushSubscription> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByUserIdAndEndpoint(UUID userId, String endpoint) {
        repository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    @Override
    public void updateLastUsedAt(UUID id, LocalDateTime lastUsedAt) {
        repository.updateLastUsedAt(id, lastUsedAt);
    }
}
