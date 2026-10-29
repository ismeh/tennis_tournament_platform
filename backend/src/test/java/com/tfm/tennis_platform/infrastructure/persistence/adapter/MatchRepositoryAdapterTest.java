package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MatchEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.MatchDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchRepositoryAdapterTest {

    @Mock
    private JpaMatchRepository matchRepository;
    @Mock
    private MatchDomainMapper mapper;
    @InjectMocks
    private MatchRepositoryAdapter adapter;

    @Nested
    class SaveTests {
        @Test
        void should_return_null_when_match_is_null() {
            assertThat(adapter.save(null)).isNull();
        }

        @Test
        void should_save_new_match_when_id_is_null() {
            Match domain = Match.builder().drawId(UUID.randomUUID()).build();
            MatchEntity entity = MatchEntity.builder().id(UUID.randomUUID()).build();
            MatchEntity saved = MatchEntity.builder().id(entity.getId()).build();
            Match mapped = Match.builder().id(entity.getId()).build();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(matchRepository.save(entity)).thenReturn(saved);
            when(mapper.toDomain(saved)).thenReturn(mapped);

            Match result = adapter.save(domain);

            assertThat(result).isEqualTo(mapped);
            verify(matchRepository).save(entity);
        }

        @Test
        void should_create_new_when_not_found_in_db() {
            UUID id = UUID.randomUUID();
            Match domain = Match.builder().id(id).build();
            MatchEntity mappedEntity = MatchEntity.builder().id(id).build();
            MatchEntity saved = MatchEntity.builder().id(id).build();
            Match mapped = Match.builder().id(id).build();

            when(matchRepository.findById(id)).thenReturn(Optional.empty());
            when(mapper.toEntity(domain)).thenReturn(mappedEntity);
            when(matchRepository.save(mappedEntity)).thenReturn(saved);
            when(mapper.toDomain(saved)).thenReturn(mapped);

            Match result = adapter.save(domain);

            assertThat(result).isEqualTo(mapped);
        }

        @Test
        void should_copy_state_and_set_next_match_references() {
            UUID id = UUID.randomUUID();
            UUID nextId = UUID.randomUUID();
            Match nextMatch = Match.builder().id(nextId).build();
            Match domain = Match.builder().id(id).nextMatch(nextMatch).build();

            MatchEntity existing = MatchEntity.builder().id(id).build();
            MatchEntity sourceEntity = MatchEntity.builder().id(id).build();
            MatchEntity nextRef = MatchEntity.builder().id(nextId).build();

            when(matchRepository.findById(id)).thenReturn(Optional.of(existing));
            when(mapper.toEntity(domain)).thenReturn(sourceEntity);
            when(matchRepository.getReferenceById(nextId)).thenReturn(nextRef);

            MatchEntity savedEntity = MatchEntity.builder().id(id).build();
            Match mapped = Match.builder().id(id).build();
            when(matchRepository.save(any(MatchEntity.class))).thenReturn(savedEntity);
            when(mapper.toDomain(savedEntity)).thenReturn(mapped);

            Match result = adapter.save(domain);

            assertThat(result).isEqualTo(mapped);
            verify(matchRepository).getReferenceById(nextId);
        }

        @Test
        void should_set_loser_next_match_reference() {
            UUID id = UUID.randomUUID();
            UUID loserNextId = UUID.randomUUID();
            Match loserNext = Match.builder().id(loserNextId).build();
            Match domain = Match.builder().id(id).loserNextMatch(loserNext).build();

            MatchEntity existing = MatchEntity.builder().id(id).build();
            MatchEntity sourceEntity = MatchEntity.builder().id(id).build();
            MatchEntity loserRef = MatchEntity.builder().id(loserNextId).build();

            when(matchRepository.findById(id)).thenReturn(Optional.of(existing));
            when(mapper.toEntity(domain)).thenReturn(sourceEntity);
            when(matchRepository.getReferenceById(loserNextId)).thenReturn(loserRef);

            MatchEntity savedEntity = MatchEntity.builder().id(id).build();
            Match mapped = Match.builder().id(id).build();
            when(matchRepository.save(any(MatchEntity.class))).thenReturn(savedEntity);
            when(mapper.toDomain(savedEntity)).thenReturn(mapped);

            Match result = adapter.save(domain);

            assertThat(result).isEqualTo(mapped);
            verify(matchRepository).getReferenceById(loserNextId);
        }
    }

    @Nested
    class SaveAllTests {
        @Test
        void should_return_empty_when_null() {
            assertThat(adapter.saveAll(null)).isEmpty();
        }

        @Test
        void should_return_empty_when_empty() {
            assertThat(adapter.saveAll(List.of())).isEmpty();
        }

        @Test
        void should_save_new_matches() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Match m1 = Match.builder().id(id1).build();
            Match m2 = Match.builder().id(id2).build();

            MatchEntity e1 = MatchEntity.builder().id(id1).build();
            MatchEntity e2 = MatchEntity.builder().id(id2).build();

            when(matchRepository.findById(id1)).thenReturn(Optional.empty());
            when(matchRepository.findById(id2)).thenReturn(Optional.empty());
            when(mapper.toEntity(m1)).thenReturn(e1);
            when(mapper.toEntity(m2)).thenReturn(e2);

            MatchEntity saved1 = MatchEntity.builder().id(id1).build();
            MatchEntity saved2 = MatchEntity.builder().id(id2).build();
            when(matchRepository.saveAll(anyList())).thenReturn(List.of(saved1, saved2));

            Match d1 = Match.builder().id(id1).build();
            Match d2 = Match.builder().id(id2).build();
            when(mapper.toDomain(saved1)).thenReturn(d1);
            when(mapper.toDomain(saved2)).thenReturn(d2);

            List<Match> result = adapter.saveAll(List.of(m1, m2));

            assertThat(result).hasSize(2);
            verify(matchRepository).saveAll(anyList());
        }

        @Test
        void should_resolve_next_match_from_local_map() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Match m1 = Match.builder().id(id1).nextMatch(Match.builder().id(id2).build()).build();
            Match m2 = Match.builder().id(id2).build();

            MatchEntity e1 = MatchEntity.builder().id(id1).build();
            MatchEntity e2 = MatchEntity.builder().id(id2).build();

            when(matchRepository.findById(id1)).thenReturn(Optional.empty());
            when(matchRepository.findById(id2)).thenReturn(Optional.empty());
            when(mapper.toEntity(m1)).thenReturn(e1);
            when(mapper.toEntity(m2)).thenReturn(e2);

            MatchEntity saved1 = MatchEntity.builder().id(id1).build();
            MatchEntity saved2 = MatchEntity.builder().id(id2).build();
            when(matchRepository.saveAll(anyList())).thenReturn(List.of(saved1, saved2));

            Match d1 = Match.builder().id(id1).build();
            Match d2 = Match.builder().id(id2).build();
            when(mapper.toDomain(saved1)).thenReturn(d1);
            when(mapper.toDomain(saved2)).thenReturn(d2);

            List<Match> result = adapter.saveAll(List.of(m1, m2));

            assertThat(result).hasSize(2);
        }

        @Test
        void should_resolve_next_match_from_repository_when_not_in_local_map() {
            UUID id1 = UUID.randomUUID();
            UUID nextId = UUID.randomUUID();
            Match m1 = Match.builder().id(id1).nextMatch(Match.builder().id(nextId).build()).build();

            MatchEntity e1 = MatchEntity.builder().id(id1).build();
            MatchEntity nextRef = MatchEntity.builder().id(nextId).build();

            when(matchRepository.findById(id1)).thenReturn(Optional.empty());
            when(mapper.toEntity(m1)).thenReturn(e1);
            when(matchRepository.getReferenceById(nextId)).thenReturn(nextRef);

            MatchEntity saved1 = MatchEntity.builder().id(id1).build();
            when(matchRepository.saveAll(anyList())).thenReturn(List.of(saved1));
            Match d1 = Match.builder().id(id1).build();
            when(mapper.toDomain(saved1)).thenReturn(d1);

            List<Match> result = adapter.saveAll(List.of(m1));

            assertThat(result).hasSize(1);
            verify(matchRepository).getReferenceById(nextId);
        }

        @Test
        void should_retry_on_optimistic_locking_failure() {
            UUID id = UUID.randomUUID();
            Match m = Match.builder().id(id).build();
            MatchEntity e = MatchEntity.builder().id(id).build();

            when(matchRepository.findById(id)).thenReturn(Optional.empty());
            when(mapper.toEntity(m)).thenReturn(e);

            MatchEntity saved = MatchEntity.builder().id(id).build();
            when(matchRepository.saveAll(anyList()))
                    .thenThrow(new ObjectOptimisticLockingFailureException("Match", id))
                    .thenReturn(List.of(saved));

            Match mapped = Match.builder().id(id).build();
            when(mapper.toDomain(saved)).thenReturn(mapped);

            List<Match> result = adapter.saveAll(List.of(m));

            assertThat(result).hasSize(1);
            verify(matchRepository, times(2)).saveAll(anyList());
        }

        @Test
        void should_throw_after_max_retries() {
            UUID id = UUID.randomUUID();
            Match m = Match.builder().id(id).build();
            MatchEntity e = MatchEntity.builder().id(id).build();

            when(matchRepository.findById(id)).thenReturn(Optional.empty());
            when(mapper.toEntity(m)).thenReturn(e);
            when(matchRepository.saveAll(anyList()))
                    .thenThrow(new ObjectOptimisticLockingFailureException("Match", id));

            assertThatThrownBy(() -> adapter.saveAll(List.of(m)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }

    @Nested
    class FindTests {
        @Test
        void should_find_by_tournament_id() {
            UUID tournamentId = UUID.randomUUID();
            MatchEntity e = MatchEntity.builder().id(UUID.randomUUID()).build();
            Match d = Match.builder().id(e.getId()).build();

            when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(e));
            when(mapper.toDomain(e)).thenReturn(d);

            List<Match> result = adapter.findByTournamentId(tournamentId);

            assertThat(result).hasSize(1).containsExactly(d);
        }

        @Test
        void should_find_by_valid_uuid_string() {
            UUID id = UUID.randomUUID();
            MatchEntity e = MatchEntity.builder().id(id).build();
            Match d = Match.builder().id(id).build();

            when(matchRepository.findById(id)).thenReturn(Optional.of(e));
            when(mapper.toDomain(e)).thenReturn(d);

            Optional<Match> result = adapter.findById(id.toString());

            assertThat(result).contains(d);
        }

        @Test
        void should_return_empty_for_invalid_uuid() {
            Optional<Match> result = adapter.findById("not-a-uuid");
            assertThat(result).isEmpty();
        }

        @Test
        void should_find_by_id_and_tournament_id() {
            UUID matchId = UUID.randomUUID();
            UUID tournamentId = UUID.randomUUID();
            MatchEntity e = MatchEntity.builder().id(matchId).build();
            Match d = Match.builder().id(matchId).build();

            when(matchRepository.findByIdAndTournamentId(matchId, tournamentId)).thenReturn(Optional.of(e));
            when(mapper.toDomain(e)).thenReturn(d);

            Optional<Match> result = adapter.findByIdAndTournamentId(matchId, tournamentId);

            assertThat(result).contains(d);
        }

        @Test
        void should_return_empty_when_match_id_is_null() {
            assertThat(adapter.findByIdAndTournamentId(null, UUID.randomUUID())).isEmpty();
        }

        @Test
        void should_return_empty_when_tournament_id_is_null() {
            assertThat(adapter.findByIdAndTournamentId(UUID.randomUUID(), null)).isEmpty();
        }
    }
}
