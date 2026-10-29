package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.domain.port.out.PlayerInvitationRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.PlayerInvitationMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPlayerInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlayerInvitationPersistenceAdapter implements PlayerInvitationRepository {

    private final JpaPlayerInvitationRepository jpaRepository;
    private final PlayerInvitationMapper mapper;

    @Override
    public PlayerInvitation save(PlayerInvitation invitation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(invitation)));
    }

    @Override
    public Optional<PlayerInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<PlayerInvitation> findByParticipantId(UUID participantId) {
        return jpaRepository.findByParticipantId(participantId).map(mapper::toDomain);
    }

    @Override
    public void markAsClaimed(UUID invitationId, UUID memberId, LocalDateTime claimedAt) {
        jpaRepository.markAsClaimed(invitationId, memberId, claimedAt);
    }
}
