package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ScheduleConfigEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ScheduleConfigDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaScheduleConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleConfigRepositoryAdapter")
class ScheduleConfigRepositoryAdapterTest {

    @Mock
    private JpaScheduleConfigRepository jpaRepository;

    @Mock
    private ScheduleConfigDomainMapper mapper;

    @InjectMocks
    private ScheduleConfigRepositoryAdapter adapter;

    private ScheduleConfig buildDomain() {
        return ScheduleConfig.builder()
                .id(UUID.randomUUID())
                .tournamentId(UUID.randomUUID())
                .matchDurationMinutes(90)
                .build();
    }

    private ScheduleConfigEntity buildEntity() {
        return ScheduleConfigEntity.builder()
                .id(UUID.randomUUID())
                .matchDurationMinutes(90)
                .build();
    }

    @Nested
    @DisplayName("findByTournamentId")
    class FindByTournamentIdTests {

        @Test
        @DisplayName("should return mapped domain when found")
        void should_return_mapped_domain() {
            UUID tournamentId = UUID.randomUUID();
            ScheduleConfigEntity entity = buildEntity();
            ScheduleConfig domain = buildDomain();

            when(jpaRepository.findByTournamentId(tournamentId))
                    .thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<ScheduleConfig> result = adapter.findByTournamentId(tournamentId);

            assertThat(result).contains(domain);
            verify(jpaRepository).findByTournamentId(tournamentId);
        }

        @Test
        @DisplayName("should return empty when not found")
        void should_return_empty_when_not_found() {
            UUID tournamentId = UUID.randomUUID();
            when(jpaRepository.findByTournamentId(tournamentId)).thenReturn(Optional.empty());

            Optional<ScheduleConfig> result = adapter.findByTournamentId(tournamentId);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should map to entity, save, and map back to domain")
        void should_save_and_map_back() {
            ScheduleConfig domain = buildDomain();
            ScheduleConfigEntity entity = buildEntity();
            ScheduleConfigEntity savedEntity = buildEntity();
            ScheduleConfig mappedDomain = buildDomain();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomain(savedEntity)).thenReturn(mappedDomain);

            ScheduleConfig result = adapter.save(domain);

            assertThat(result).isEqualTo(mappedDomain);
            verify(mapper).toEntity(domain);
            verify(jpaRepository).save(entity);
            verify(mapper).toDomain(savedEntity);
        }
    }

    @Nested
    @DisplayName("deleteByTournamentId")
    class DeleteByTournamentIdTests {

        @Test
        @DisplayName("should delegate to repository")
        void should_delegate_to_repository() {
            UUID tournamentId = UUID.randomUUID();

            adapter.deleteByTournamentId(tournamentId);

            verify(jpaRepository).deleteByTournamentId(tournamentId);
            verifyNoInteractions(mapper);
        }
    }
}
