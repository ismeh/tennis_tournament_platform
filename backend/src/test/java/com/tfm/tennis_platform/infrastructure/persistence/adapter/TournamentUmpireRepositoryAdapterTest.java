package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentUmpireEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentUmpireMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentUmpireRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentUmpireRepositoryAdapter")
class TournamentUmpireRepositoryAdapterTest {

    @Mock
    private JpaTournamentUmpireRepository jpaRepository;

    @Mock
    private TournamentUmpireMapper mapper;

    @InjectMocks
    private TournamentUmpireRepositoryAdapter adapter;

    private TournamentUmpire buildDomain() {
        return TournamentUmpire.builder()
                .id(UUID.randomUUID())
                .tournamentId(UUID.randomUUID())
                .umpireId(UUID.randomUUID())
                .umpireEmail("umpire@test.com")
                .umpireFirstName("Carlos")
                .umpireLastName("Lopez")
                .assignedAt(LocalDateTime.of(2026, 3, 15, 9, 0))
                .build();
    }

    private TournamentUmpireEntity buildEntity() {
        return TournamentUmpireEntity.builder()
                .id(UUID.randomUUID())
                .assignedAt(LocalDateTime.of(2026, 3, 15, 9, 0))
                .build();
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should map to entity, save, and map back to domain")
        void should_save_and_map_back() {
            TournamentUmpire domain = buildDomain();
            TournamentUmpireEntity entity = buildEntity();
            TournamentUmpireEntity savedEntity = buildEntity();
            TournamentUmpire mappedDomain = buildDomain();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomain(savedEntity)).thenReturn(mappedDomain);

            TournamentUmpire result = adapter.save(domain);

            assertThat(result).isEqualTo(mappedDomain);
            verify(mapper).toEntity(domain);
            verify(jpaRepository).save(entity);
            verify(mapper).toDomain(savedEntity);
        }
    }

    @Nested
    @DisplayName("findByTournamentId")
    class FindByTournamentIdTests {

        @Test
        @DisplayName("should return list of mapped domains when found")
        void should_return_mapped_list() {
            UUID tournamentId = UUID.randomUUID();
            TournamentUmpireEntity entity1 = buildEntity();
            TournamentUmpireEntity entity2 = buildEntity();
            TournamentUmpire domain1 = buildDomain();
            TournamentUmpire domain2 = buildDomain();

            when(jpaRepository.findByTournamentIdOrderByAssignedAtAsc(tournamentId))
                    .thenReturn(List.of(entity1, entity2));
            when(mapper.toDomain(entity1)).thenReturn(domain1);
            when(mapper.toDomain(entity2)).thenReturn(domain2);

            List<TournamentUmpire> result = adapter.findByTournamentId(tournamentId);

            assertThat(result).hasSize(2).containsExactly(domain1, domain2);
            verify(jpaRepository).findByTournamentIdOrderByAssignedAtAsc(tournamentId);
        }

        @Test
        @DisplayName("should return empty list when no umpires found")
        void should_return_empty_list() {
            UUID tournamentId = UUID.randomUUID();
            when(jpaRepository.findByTournamentIdOrderByAssignedAtAsc(tournamentId))
                    .thenReturn(List.of());

            List<TournamentUmpire> result = adapter.findByTournamentId(tournamentId);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("findByTournamentIdAndUmpireId")
    class FindByTournamentIdAndUmpireIdTests {

        @Test
        @DisplayName("should return mapped domain when found")
        void should_return_mapped_domain() {
            UUID tournamentId = UUID.randomUUID();
            UUID umpireId = UUID.randomUUID();
            TournamentUmpireEntity entity = buildEntity();
            TournamentUmpire domain = buildDomain();

            when(jpaRepository.findByTournamentIdAndUmpireId(tournamentId, umpireId))
                    .thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<TournamentUmpire> result =
                    adapter.findByTournamentIdAndUmpireId(tournamentId, umpireId);

            assertThat(result).contains(domain);
            verify(jpaRepository).findByTournamentIdAndUmpireId(tournamentId, umpireId);
        }

        @Test
        @DisplayName("should return empty when not found")
        void should_return_empty_when_not_found() {
            UUID tournamentId = UUID.randomUUID();
            UUID umpireId = UUID.randomUUID();
            when(jpaRepository.findByTournamentIdAndUmpireId(tournamentId, umpireId))
                    .thenReturn(Optional.empty());

            Optional<TournamentUmpire> result =
                    adapter.findByTournamentIdAndUmpireId(tournamentId, umpireId);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("existsByTournamentIdAndUmpireId")
    class ExistsByTournamentIdAndUmpireIdTests {

        @Test
        @DisplayName("should return true when umpire exists in tournament")
        void should_return_true_when_exists() {
            UUID tournamentId = UUID.randomUUID();
            UUID umpireId = UUID.randomUUID();
            when(jpaRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId))
                    .thenReturn(true);

            boolean result = adapter.existsByTournamentIdAndUmpireId(tournamentId, umpireId);

            assertThat(result).isTrue();
            verify(jpaRepository).existsByTournamentIdAndUmpireId(tournamentId, umpireId);
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("should return false when umpire does not exist in tournament")
        void should_return_false_when_not_exists() {
            UUID tournamentId = UUID.randomUUID();
            UUID umpireId = UUID.randomUUID();
            when(jpaRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId))
                    .thenReturn(false);

            boolean result = adapter.existsByTournamentIdAndUmpireId(tournamentId, umpireId);

            assertThat(result).isFalse();
            verify(jpaRepository).existsByTournamentIdAndUmpireId(tournamentId, umpireId);
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("deleteByTournamentIdAndUmpireId")
    class DeleteByTournamentIdAndUmpireIdTests {

        @Test
        @DisplayName("should delegate to repository")
        void should_delegate_to_repository() {
            UUID tournamentId = UUID.randomUUID();
            UUID umpireId = UUID.randomUUID();

            adapter.deleteByTournamentIdAndUmpireId(tournamentId, umpireId);

            verify(jpaRepository).deleteByTournamentIdAndUmpireId(tournamentId, umpireId);
            verifyNoInteractions(mapper);
        }
    }
}
