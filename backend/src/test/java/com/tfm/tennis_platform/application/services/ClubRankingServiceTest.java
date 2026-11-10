package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.models.club.ClubRankingEntry;
import com.tfm.tennis_platform.domain.port.out.ClubRankingRepository;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubRankingServiceTest {

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubRankingRepository clubRankingRepository;

    @InjectMocks
    private ClubRankingService clubRankingService;

    @Test
    void find_ranking_returns_entries_with_shared_positions_for_ties() {
        UUID clubId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).slug("club-norte").publicPageEnabled(true).build();
        List<ClubRankingEntry> entries = List.of(
                entry(UUID.randomUUID(), 120L),
                entry(UUID.randomUUID(), 120L),
                entry(UUID.randomUUID(), 90L)
        );

        when(clubRepository.findBySlug("club-norte")).thenReturn(Optional.of(club));
        when(clubRankingRepository.findClubRanking(clubId, null)).thenReturn(entries);

        List<ClubRankingEntry> ranking = clubRankingService.findRanking("club-norte", null);

        assertThat(ranking).extracting(ClubRankingEntry::position).containsExactly(1, 1, 3);
    }

    @Test
    void find_ranking_normalizes_gender_filter() {
        UUID clubId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).slug("club-norte").publicPageEnabled(true).build();

        when(clubRepository.findBySlug("club-norte")).thenReturn(Optional.of(club));
        when(clubRankingRepository.findClubRanking(clubId, "FEMALE")).thenReturn(List.of());

        clubRankingService.findRanking("club-norte", " female ");

        verify(clubRankingRepository).findClubRanking(clubId, "FEMALE");
    }

    @Test
    void find_ranking_ignores_unknown_gender_filter() {
        UUID clubId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).slug("club-norte").publicPageEnabled(true).build();

        when(clubRepository.findBySlug("club-norte")).thenReturn(Optional.of(club));
        when(clubRankingRepository.findClubRanking(clubId, null)).thenReturn(List.of());

        clubRankingService.findRanking("club-norte", "unknown");

        verify(clubRankingRepository).findClubRanking(clubId, null);
    }

    @Test
    void find_ranking_treats_null_points_as_zero() {
        UUID clubId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).slug("club-norte").publicPageEnabled(true).build();
        List<ClubRankingEntry> entries = List.of(
                entry(UUID.randomUUID(), null),
                entry(UUID.randomUUID(), 10L)
        );

        when(clubRepository.findBySlug("club-norte")).thenReturn(Optional.of(club));
        when(clubRankingRepository.findClubRanking(clubId, null)).thenReturn(entries);

        List<ClubRankingEntry> ranking = clubRankingService.findRanking("club-norte", null);

        assertThat(ranking).extracting(ClubRankingEntry::position).containsExactly(1, 2);
    }

    @Test
    void find_ranking_by_club_id_does_not_require_public_page() {
        UUID clubId = UUID.randomUUID();
        List<ClubRankingEntry> entries = List.of(entry(UUID.randomUUID(), 50L));
        when(clubRankingRepository.findClubRanking(clubId, null)).thenReturn(entries);

        List<ClubRankingEntry> ranking = clubRankingService.findRankingByClubId(clubId, null);

        assertThat(ranking).hasSize(1);
        assertThat(ranking.getFirst().position()).isEqualTo(1);
    }

    @Test
    void find_ranking_rejects_unknown_club() {
        when(clubRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubRankingService.findRanking("missing", null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(clubRankingRepository);
    }

    @Test
    void find_ranking_rejects_club_with_disabled_public_page() {
        Club club = Club.builder().id(UUID.randomUUID()).slug("private").publicPageEnabled(false).build();
        when(clubRepository.findBySlug("private")).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> clubRankingService.findRanking("private", null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(clubRankingRepository);
    }

    private ClubRankingEntry entry(UUID personId, Long points) {
        return new ClubRankingEntry(null, personId, "Ana", "Lopez", "FEMALE", "LIC-1", points, 3L, 5L);
    }
}
