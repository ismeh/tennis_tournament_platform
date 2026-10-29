package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CalendarRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarRepositoryAdapter")
class CalendarRepositoryAdapterTest {

    @Mock private EntityManager entityManager;
    @Mock private JpaMatchRepository matchRepository;
    @Mock private JpaMemberRepository memberRepository;

    @InjectMocks
    private CalendarRepositoryAdapter adapter;

    @SuppressWarnings("unchecked")
    private void mockEmptyTournamentQuery() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<TournamentEntity> query = mock(CriteriaQuery.class);
        Root<TournamentEntity> root = mock(Root.class);
        Join<Object, Object> join = mock(Join.class);
        TypedQuery<TournamentEntity> typedQuery = mock(TypedQuery.class);
        Predicate predicate = mock(Predicate.class);
        Path<LocalDate> datePath = mock(Path.class);

        doReturn(cb).when(entityManager).getCriteriaBuilder();
        doReturn(query).when(cb).createQuery(TournamentEntity.class);
        doReturn(root).when(query).from(TournamentEntity.class);
        doReturn(join).when(root).join(eq("createdBy"), any(JoinType.class));

        lenient().doReturn(datePath).when(root).get(any(String.class));
        lenient().doReturn(datePath).when(join).get(any(String.class));
        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(any(Path.class), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(any(Path.class), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).or(any(Predicate[].class));
        lenient().doReturn(predicate).when(cb).and(any(Predicate[].class));
        lenient().doReturn(query).when(query).select(any());
        lenient().doReturn(query).when(query).where(any(Predicate[].class));
        lenient().doReturn(query).when(query).orderBy(any(Order[].class));
        doReturn(typedQuery).when(entityManager).createQuery(query);
        doReturn(List.of()).when(typedQuery).getResultList();
    }

    @Nested
    @DisplayName("findPublishedTournamentsPaginated")
    class FindPublishedTournamentsPaginatedTests {

        @Test
        @DisplayName("should return empty page when no tournaments match")
        void should_return_empty_page() {
            mockEmptyTournamentQuery();

            CalendarRepository.PageResult<TournamentCalendarItem> result = adapter.findPublishedTournamentsPaginated(
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    List.of(TournamentStatus.OPEN), null, null, null, null, null, 0, 10);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findPublishedTournaments")
    class FindPublishedTournamentsTests {

        @Test
        @DisplayName("should return empty list when no tournaments match")
        void should_return_empty_list() {
            mockEmptyTournamentQuery();

            List<TournamentCalendarItem> result = adapter.findPublishedTournaments(
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    List.of(TournamentStatus.OPEN), null, null, null, null, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findScheduledMatchesForPlayer")
    class FindScheduledMatchesForPlayerTests {

        @Test
        @DisplayName("should return empty list when member not found")
        void should_return_empty_when_member_not_found() {
            when(memberRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            List<PlayerMatchCalendarItem> result = adapter.findScheduledMatchesForPlayer(
                    "unknown@test.com", LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                    List.of(TournamentStatus.IN_PROGRESS));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when member has no personId")
        void should_return_empty_when_no_person_id() {
            MemberEntity member = mock(MemberEntity.class);
            when(member.getPersonId()).thenReturn(null);
            when(memberRepository.findByEmail("player@test.com")).thenReturn(Optional.of(member));

            List<PlayerMatchCalendarItem> result = adapter.findScheduledMatchesForPlayer(
                    "player@test.com", LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                    List.of(TournamentStatus.IN_PROGRESS));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findMyTournaments")
    class FindMyTournamentsTests {

        @Test
        @DisplayName("should return empty list when organizer email is blank")
        void should_return_empty_when_blank_email() {
            List<TournamentCalendarItem> result = adapter.findMyTournaments(
                    "", LocalDate.now(), LocalDate.now().plusDays(30));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when organizer email is null")
        void should_return_empty_when_null_email() {
            List<TournamentCalendarItem> result = adapter.findMyTournaments(
                    null, LocalDate.now(), LocalDate.now().plusDays(30));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should execute query with valid email")
        void should_execute_query_with_valid_email() {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<TournamentEntity> query = mock(CriteriaQuery.class);
            Root<TournamentEntity> root = mock(Root.class);
            Join<Object, Object> join = mock(Join.class);
            TypedQuery<TournamentEntity> typedQuery = mock(TypedQuery.class);
            Predicate predicate = mock(Predicate.class);
            Path stringPath = mock(Path.class);

            doReturn(cb).when(entityManager).getCriteriaBuilder();
            doReturn(query).when(cb).createQuery(TournamentEntity.class);
            doReturn(root).when(query).from(TournamentEntity.class);
            doReturn(join).when(root).join(eq("createdBy"), any(JoinType.class));
            lenient().doReturn(stringPath).when(join).get(any(String.class));
            lenient().doReturn(stringPath).when(cb).lower(any());
            lenient().doReturn(predicate).when(cb).equal(any(), any());
            lenient().doReturn(query).when(query).select(any());
            lenient().doReturn(query).when(query).where(any(Predicate[].class));
            lenient().doReturn(query).when(query).orderBy(any(Order[].class));
            doReturn(typedQuery).when(entityManager).createQuery(query);
            doReturn(List.of()).when(typedQuery).getResultList();

            List<TournamentCalendarItem> result = adapter.findMyTournaments(
                    "organizer@test.com", null, null);

            assertThat(result).isEmpty();
        }
    }
}
