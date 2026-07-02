package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.domain.models.ranking.ProfessionalRankingEntry;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ProPlayerMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMatchRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.TournamentRankingProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingRepositoryAdapter")
class RankingRepositoryAdapterTest {

    @Mock private JpaProPlayerRepository proPlayerRepository;
    @Mock private JpaMatchRepository matchRepository;
    @Mock private ProPlayerMapper proPlayerMapper;

    @InjectMocks
    private RankingRepositoryAdapter adapter;

    private ProPlayerEntity buildProPlayerEntity() {
        return ProPlayerEntity.builder()
                .id(1)
                .name("Pablo, Carlos")
                .license("LIC001")
                .rankingPosition(1)
                .points(1500)
                .ageCategory("Senior")
                .clubName("Club Madrid")
                .birthDate(LocalDate.of(1990, 1, 15))
                .gender("MALE")
                .build();
    }

    private ProPlayer buildProPlayerDomain() {
        return ProPlayer.builder()
                .id(1)
                .fullName("Pablo Carlos")
                .firstName("Carlos")
                .lastName("Pablo")
                .license("LIC001")
                .rankingPosition(1)
                .points(1500)
                .ageCategory("Senior")
                .clubName("Club Madrid")
                .birthDate(LocalDate.of(1990, 1, 15))
                .gender("MALE")
                .build();
    }

    @Nested
    @DisplayName("findProfessionalRanking")
    class FindProfessionalRankingTests {

        @Test
        @DisplayName("should return paginated professional ranking")
        void should_return_paginated_ranking() {
            ProPlayerEntity entity = buildProPlayerEntity();
            ProPlayer domain = buildProPlayerDomain();
            Page<ProPlayerEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);

            when(proPlayerRepository.findRanking(eq("MALE"), eq("Senior"), any(PageRequest.class))).thenReturn(page);
            when(proPlayerMapper.toDomain(entity)).thenReturn(domain);

            RankingPage<ProfessionalRankingEntry> result = adapter.findProfessionalRanking(
                    "MALE", "Senior", 0, 10, "rankingPosition", "asc");

            assertThat(result.items()).hasSize(1);
            assertThat(result.totalItems()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
            verify(proPlayerRepository).findRanking(eq("MALE"), eq("Senior"), any(PageRequest.class));
        }

        @Test
        @DisplayName("should return empty ranking when no players")
        void should_return_empty_ranking() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            RankingPage<ProfessionalRankingEntry> result = adapter.findProfessionalRanking(
                    null, null, 0, 10, null, null);

            assertThat(result.items()).isEmpty();
            assertThat(result.totalItems()).isEqualTo(0);
        }

        @Test
        @DisplayName("should sort by name")
        void should_sort_by_name() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            adapter.findProfessionalRanking(null, null, 0, 10, "name", "desc");

            verify(proPlayerRepository).findRanking(any(), any(), any(PageRequest.class));
        }

        @Test
        @DisplayName("should sort by points")
        void should_sort_by_points() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            adapter.findProfessionalRanking(null, null, 0, 10, "points", "asc");

            verify(proPlayerRepository).findRanking(any(), any(), any(PageRequest.class));
        }

        @Test
        @DisplayName("should sort by category")
        void should_sort_by_category() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            adapter.findProfessionalRanking(null, null, 0, 10, "category", "asc");

            verify(proPlayerRepository).findRanking(any(), any(), any(PageRequest.class));
        }

        @Test
        @DisplayName("should sort by gender")
        void should_sort_by_gender() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            adapter.findProfessionalRanking(null, null, 0, 10, "gender", "asc");

            verify(proPlayerRepository).findRanking(any(), any(), any(PageRequest.class));
        }

        @Test
        @DisplayName("should handle null sortBy and sortDirection")
        void should_handle_null_sort_params() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            RankingPage<ProfessionalRankingEntry> result = adapter.findProfessionalRanking(
                    null, null, 0, 10, null, null);

            assertThat(result.sortBy()).isNull();
            assertThat(result.sortDirection()).isNull();
        }

        @Test
        @DisplayName("should handle blank sortBy")
        void should_handle_blank_sort_by() {
            Page<ProPlayerEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(proPlayerRepository.findRanking(any(), any(), any(PageRequest.class))).thenReturn(emptyPage);

            adapter.findProfessionalRanking(null, null, 0, 10, "  ", "asc");

            verify(proPlayerRepository).findRanking(any(), any(), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("findTournamentRanking")
    class FindTournamentRankingTests {

        @Test
        @DisplayName("should return tournament ranking entries")
        void should_return_tournament_ranking() {
            UUID tournamentId = UUID.randomUUID();
            TournamentRankingProjection projection = new TournamentRankingProjection() {
                @Override public UUID getParticipantId() { return UUID.randomUUID(); }
                @Override public String getLicense() { return "LIC001"; }
                @Override public String getFirstName() { return "Carlos"; }
                @Override public String getLastName() { return "Pablo"; }
                @Override public String getGender() { return "MALE"; }
                @Override public Long getVictories() { return 5L; }
            };

            when(matchRepository.findTournamentRanking(tournamentId, "MALE", 1)).thenReturn(List.of(projection));

            List<TournamentRankingEntry> result = adapter.findTournamentRanking(tournamentId, "MALE", 1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).firstName()).isEqualTo("Carlos");
            assertThat(result.get(0).lastName()).isEqualTo("Pablo");
            assertThat(result.get(0).victories()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should return empty list when no projections")
        void should_return_empty_when_no_projections() {
            UUID tournamentId = UUID.randomUUID();
            when(matchRepository.findTournamentRanking(tournamentId, null, null)).thenReturn(List.of());

            List<TournamentRankingEntry> result = adapter.findTournamentRanking(tournamentId, null, null);

            assertThat(result).isEmpty();
        }
    }
}
