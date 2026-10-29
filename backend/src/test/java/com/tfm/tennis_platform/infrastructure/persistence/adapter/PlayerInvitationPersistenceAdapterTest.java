package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PlayerInvitationEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.PlayerInvitationMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPlayerInvitationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerInvitationPersistenceAdapter")
class PlayerInvitationPersistenceAdapterTest {

    @Mock
    private JpaPlayerInvitationRepository jpaRepository;

    @Mock
    private PlayerInvitationMapper mapper;

    @InjectMocks
    private PlayerInvitationPersistenceAdapter adapter;

    private PlayerInvitation buildDomain() {
        return PlayerInvitation.builder()
                .id(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .tokenHash("abc123hash")
                .expiresAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .claimedAt(null)
                .claimedByMemberId(null)
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    private PlayerInvitationEntity buildEntity() {
        return PlayerInvitationEntity.builder()
                .id(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .tokenHash("abc123hash")
                .expiresAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .claimedAt(null)
                .claimedByMember(null)
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should map to entity, save, and map back to domain")
        void should_save_and_map_back() {
            PlayerInvitation domain = buildDomain();
            PlayerInvitationEntity entity = buildEntity();
            PlayerInvitationEntity savedEntity = buildEntity();
            PlayerInvitation mappedDomain = buildDomain();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomain(savedEntity)).thenReturn(mappedDomain);

            PlayerInvitation result = adapter.save(domain);

            assertThat(result).isEqualTo(mappedDomain);
            verify(mapper).toEntity(domain);
            verify(jpaRepository).save(entity);
            verify(mapper).toDomain(savedEntity);
        }
    }

    @Nested
    @DisplayName("findByTokenHash")
    class FindByTokenHashTests {

        @Test
        @DisplayName("should return mapped domain when found")
        void should_return_mapped_domain() {
            PlayerInvitationEntity entity = buildEntity();
            PlayerInvitation domain = buildDomain();

            when(jpaRepository.findByTokenHash("abc123hash"))
                    .thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<PlayerInvitation> result = adapter.findByTokenHash("abc123hash");

            assertThat(result).contains(domain);
            verify(jpaRepository).findByTokenHash("abc123hash");
        }

        @Test
        @DisplayName("should return empty when token hash not found")
        void should_return_empty_when_not_found() {
            when(jpaRepository.findByTokenHash("unknown")).thenReturn(Optional.empty());

            Optional<PlayerInvitation> result = adapter.findByTokenHash("unknown");

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("findByParticipantId")
    class FindByParticipantIdTests {

        @Test
        @DisplayName("should return mapped domain when found")
        void should_return_mapped_domain() {
            UUID participantId = UUID.randomUUID();
            PlayerInvitationEntity entity = buildEntity();
            PlayerInvitation domain = buildDomain();

            when(jpaRepository.findByParticipantId(participantId))
                    .thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<PlayerInvitation> result = adapter.findByParticipantId(participantId);

            assertThat(result).contains(domain);
            verify(jpaRepository).findByParticipantId(participantId);
        }

        @Test
        @DisplayName("should return empty when participant not found")
        void should_return_empty_when_not_found() {
            UUID participantId = UUID.randomUUID();
            when(jpaRepository.findByParticipantId(participantId)).thenReturn(Optional.empty());

            Optional<PlayerInvitation> result = adapter.findByParticipantId(participantId);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("markAsClaimed")
    class MarkAsClaimedTests {

        @Test
        @DisplayName("should delegate to repository")
        void should_delegate_to_repository() {
            UUID invitationId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            LocalDateTime claimedAt = LocalDateTime.of(2026, 6, 1, 12, 0);

            adapter.markAsClaimed(invitationId, memberId, claimedAt);

            verify(jpaRepository).markAsClaimed(invitationId, memberId, claimedAt);
            verifyNoInteractions(mapper);
        }
    }
}
