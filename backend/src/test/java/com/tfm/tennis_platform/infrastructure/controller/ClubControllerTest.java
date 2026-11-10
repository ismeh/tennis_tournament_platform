package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.ClubQueryService;
import com.tfm.tennis_platform.application.services.ClubRankingService;
import com.tfm.tennis_platform.application.services.ClubService;
import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.models.ClubMembership;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.club.ClubMemberView;
import com.tfm.tennis_platform.domain.models.club.ClubMembershipSummary;
import com.tfm.tennis_platform.domain.models.club.ClubRankingEntry;
import com.tfm.tennis_platform.domain.models.enums.ClubPermission;
import com.tfm.tennis_platform.domain.models.enums.ClubRole;
import com.tfm.tennis_platform.infrastructure.controller.dto.AddClubMemberRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubManagementResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubMemberResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubMembershipResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubPageResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubProfileRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubRankingResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubRolePermissionsResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.UpdateClubMemberPermissionsRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.UpdateClubMemberRoleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubQueryService clubQueryService;
    @Mock
    private ClubService clubService;
    @Mock
    private ClubRankingService clubRankingService;
    @InjectMocks
    private ClubController controller;

    private static final Principal PRINCIPAL = () -> "owner@test.com";

    @Test
    void should_list_public_clubs() {
        Club club = Club.builder().id(UUID.randomUUID()).name("Club Norte").slug("club-norte").build();
        when(clubQueryService.findPublicClubs("norte")).thenReturn(List.of(club));
        when(clubQueryService.countMembers(club.getId())).thenReturn(12L);

        ResponseEntity<List<ClubResponse>> response = controller.listPublicClubs("norte");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().memberCount()).isEqualTo(12L);
    }

    @Test
    void should_return_public_club_page() {
        Club club = club();
        when(clubQueryService.findPublicBySlugOrThrow("club-norte")).thenReturn(club);
        when(clubQueryService.countMembers(club.getId())).thenReturn(7L);

        ResponseEntity<ClubPageResponse> response = controller.getPublicPage("club-norte");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().slug()).isEqualTo("club-norte");
        assertThat(response.getBody().memberCount()).isEqualTo(7L);
    }

    @Test
    void should_return_public_ranking() {
        ClubRankingEntry entry = new ClubRankingEntry(
                1, UUID.randomUUID(), "Ana", "Lopez", "FEMALE", "LIC-1", 100L, 4L, 6L);
        when(clubRankingService.findRanking("club-norte", "FEMALE")).thenReturn(List.of(entry));

        ResponseEntity<List<ClubRankingResponse>> response =
                controller.getPublicRanking("club-norte", "FEMALE");

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().position()).isEqualTo(1);
        assertThat(response.getBody().getFirst().firstName()).isEqualTo("Ana");
    }

    @Test
    void should_return_public_tournaments() {
        Club club = club();
        TournamentSummary summary = new TournamentSummary(
                UUID.randomUUID(), "Open Norte", null, null, null, null, null,
                com.tfm.tennis_platform.domain.models.enums.Surface.CLAY, 32, "Madrid",
                com.tfm.tennis_platform.domain.models.enums.TournamentStatus.OPEN, false);
        when(clubQueryService.findPublicBySlugOrThrow("club-norte")).thenReturn(club);
        when(clubQueryService.findTournaments(club.getId())).thenReturn(List.of(summary));

        ResponseEntity<List<com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse>>
                response = controller.getPublicTournaments("club-norte");

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().formalName()).isEqualTo("Open Norte");
    }

    @Test
    void should_return_permission_catalog_for_every_role() {
        ResponseEntity<List<ClubRolePermissionsResponse>> response = controller.getPermissionCatalog();

        assertThat(response.getBody()).hasSize(ClubRole.values().length);
        assertThat(response.getBody())
                .extracting(ClubRolePermissionsResponse::role)
                .containsExactly(ClubRole.values());
        assertThat(response.getBody().getFirst().permissions()).containsAll(ClubPermission.all());
    }

    @Test
    void should_create_club() {
        Club club = club();
        when(clubService.create(any(), eq("owner@test.com"))).thenReturn(club);
        when(clubQueryService.countMembers(club.getId())).thenReturn(1L);

        ResponseEntity<ClubPageResponse> response = controller.create(profileRequest(), PRINCIPAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().slug()).isEqualTo("club-norte");
        verify(clubService).create(any(), eq("owner@test.com"));
    }

    @Test
    void should_return_my_clubs() {
        ClubMembershipSummary summary = new ClubMembershipSummary(
                UUID.randomUUID(), UUID.randomUUID(), "Club Norte", "club-norte",
                null, "#16a34a", "Madrid", "España", ClubRole.OWNER,
                ClubPermission.all(), 5L);
        when(clubService.findMyClubs("owner@test.com")).thenReturn(List.of(summary));

        ResponseEntity<List<ClubMembershipResponse>> response = controller.getMyClubs(PRINCIPAL);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().role()).isEqualTo(ClubRole.OWNER);
    }

    @Test
    void should_return_management_data_with_members_when_allowed() {
        Club club = club();
        ClubMembership membership = membership(club.getId(), ClubRole.ADMIN);
        ClubMemberView member = memberView();
        when(clubService.getManagementData(club.getId(), "owner@test.com"))
                .thenReturn(new ClubService.ClubManagementData(club, membership, 4L));
        when(clubService.findMembers(club.getId(), "owner@test.com")).thenReturn(List.of(member));

        ResponseEntity<ClubManagementResponse> response =
                controller.getManagement(club.getId(), PRINCIPAL);

        assertThat(response.getBody().club().memberCount()).isEqualTo(4L);
        assertThat(response.getBody().membership().role()).isEqualTo(ClubRole.ADMIN);
        assertThat(response.getBody().members()).hasSize(1);
    }

    @Test
    void should_return_internal_ranking_for_members() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.MEMBER);
        ClubRankingEntry entry = new ClubRankingEntry(
                1, UUID.randomUUID(), "Ana", "Lopez", "FEMALE", "LIC-1", 100L, 4L, 6L);
        when(clubService.getMembership(clubId, "owner@test.com")).thenReturn(membership);
        when(clubRankingService.findRankingByClubId(clubId, null)).thenReturn(List.of(entry));

        ResponseEntity<List<ClubRankingResponse>> response =
                controller.getManagementRanking(clubId, null, PRINCIPAL);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().position()).isEqualTo(1);
    }

    @Test
    void should_return_internal_tournaments_for_members() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.MEMBER);
        TournamentSummary summary = new TournamentSummary(
                UUID.randomUUID(), "Open Norte", null, null, null, null, null,
                com.tfm.tennis_platform.domain.models.enums.Surface.CLAY, 32, "Madrid",
                com.tfm.tennis_platform.domain.models.enums.TournamentStatus.OPEN, false);
        when(clubService.getMembership(clubId, "owner@test.com")).thenReturn(membership);
        when(clubQueryService.findTournaments(clubId)).thenReturn(List.of(summary));

        ResponseEntity<List<com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse>>
                response = controller.getManagementTournaments(clubId, PRINCIPAL);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().formalName()).isEqualTo("Open Norte");
    }

    @Test
    void should_hide_members_when_role_cannot_view_them() {        Club club = club();
        ClubMembership membership = membership(club.getId(), ClubRole.MEMBER);
        when(clubService.getManagementData(club.getId(), "owner@test.com"))
                .thenReturn(new ClubService.ClubManagementData(club, membership, 4L));

        ResponseEntity<ClubManagementResponse> response =
                controller.getManagement(club.getId(), PRINCIPAL);

        assertThat(response.getBody().members()).isEmpty();
    }

    @Test
    void should_update_club_page() {
        Club club = club();
        when(clubService.updatePage(eq(club.getId()), any(), eq("owner@test.com"))).thenReturn(club);
        when(clubQueryService.countMembers(club.getId())).thenReturn(2L);

        ResponseEntity<ClubPageResponse> response =
                controller.updatePage(club.getId(), profileRequest(), PRINCIPAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Club Norte");
    }

    @Test
    void should_list_members() {
        ClubMemberView member = memberView();
        when(clubService.findMembers(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "owner@test.com")).thenReturn(List.of(member));

        ResponseEntity<List<ClubMemberResponse>> response = controller.getMembers(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), PRINCIPAL);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().email()).isEqualTo("member@test.com");
    }

    @Test
    void should_add_member() {
        UUID clubId = UUID.randomUUID();
        ClubMemberView member = memberView();
        when(clubService.addMember(clubId, "member@test.com", ClubRole.COACH, "owner@test.com"))
                .thenReturn(member);

        ResponseEntity<ClubMemberResponse> response = controller.addMember(
                clubId, new AddClubMemberRequest("member@test.com", ClubRole.COACH), PRINCIPAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().role()).isEqualTo(ClubRole.COACH);
    }

    @Test
    void should_update_member_role() {
        UUID clubId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        ClubMemberView member = memberView();
        when(clubService.updateMemberRole(clubId, membershipId, ClubRole.ADMIN, "owner@test.com"))
                .thenReturn(member);

        ResponseEntity<ClubMemberResponse> response = controller.updateMemberRole(
                clubId, membershipId, new UpdateClubMemberRoleRequest(ClubRole.ADMIN), PRINCIPAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(clubService).updateMemberRole(clubId, membershipId, ClubRole.ADMIN, "owner@test.com");
    }

    @Test
    void should_update_member_permissions() {
        UUID clubId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        ClubMemberView member = memberView();
        when(clubService.updateMemberPermissions(
                eq(clubId), eq(membershipId), any(), eq("owner@test.com"))).thenReturn(member);

        ResponseEntity<ClubMemberResponse> response = controller.updateMemberPermissions(
                clubId,
                membershipId,
                new UpdateClubMemberPermissionsRequest(EnumSet.of(ClubPermission.VIEW_MEMBERS)),
                PRINCIPAL
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(clubService).updateMemberPermissions(
                eq(clubId), eq(membershipId), eq(EnumSet.of(ClubPermission.VIEW_MEMBERS)), eq("owner@test.com"));
    }

    @Test
    void should_remove_member() {
        UUID clubId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.removeMember(clubId, membershipId, PRINCIPAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(clubService).removeMember(clubId, membershipId, "owner@test.com");
    }

    @Test
    void should_search_clubs() {
        Club club = club();
        when(clubQueryService.search("norte")).thenReturn(List.of(club));
        when(clubQueryService.countMembers(club.getId())).thenReturn(2L);

        ResponseEntity<List<ClubResponse>> response = controller.search("norte");

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().name()).isEqualTo("Club Norte");
    }

    private Club club() {
        return Club.builder()
                .id(UUID.randomUUID())
                .name("Club Norte")
                .slug("club-norte")
                .tagline("Tenis para todos")
                .primaryColor("#16a34a")
                .secondaryColor("#0284c7")
                .publicPageEnabled(true)
                .build();
    }

    private ClubMembership membership(UUID clubId, ClubRole role) {
        return ClubMembership.builder()
                .id(UUID.randomUUID())
                .clubId(clubId)
                .memberId(UUID.randomUUID())
                .email("owner@test.com")
                .role(role)
                .customPermissions(EnumSet.noneOf(ClubPermission.class))
                .build();
    }

    private ClubMemberView memberView() {
        return new ClubMemberView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "member@test.com",
                "Ana",
                "Lopez",
                ClubRole.COACH,
                EnumSet.noneOf(ClubPermission.class),
                EnumSet.of(ClubPermission.VIEW_MEMBERS),
                null
        );
    }

    private ClubProfileRequest profileRequest() {
        return new ClubProfileRequest(
                "Club Norte", "club-norte", "Tenis para todos", null,
                null, null, null, null, "Madrid", "España",
                null, null, null, null, null, null, null, true
        );
    }
}
