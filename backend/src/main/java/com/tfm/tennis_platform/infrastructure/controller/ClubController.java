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
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.UpdateClubMemberPermissionsRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.UpdateClubMemberRoleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubQueryService clubQueryService;
    private final ClubService clubService;
    private final ClubRankingService clubRankingService;

    /**
     * Lightweight search used by autocomplete widgets. Kept for backwards compatibility.
     */
    @GetMapping
    public ResponseEntity<List<ClubResponse>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(clubQueryService.search(q).stream()
                .map(this::toClubResponse)
                .toList());
    }

    @GetMapping("/public")
    public ResponseEntity<List<ClubResponse>> listPublicClubs(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(clubQueryService.findPublicClubs(q).stream()
                .map(this::toClubResponse)
                .toList());
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<ClubPageResponse> getPublicPage(@PathVariable String slug) {
        return ResponseEntity.ok(toPageResponse(clubQueryService.findPublicBySlugOrThrow(slug)));
    }

    @GetMapping("/public/{slug}/rankings")
    public ResponseEntity<List<ClubRankingResponse>> getPublicRanking(
            @PathVariable String slug,
            @RequestParam(required = false) String gender
    ) {
        return ResponseEntity.ok(clubRankingService.findRanking(slug, gender).stream()
                .map(ClubController::toRankingResponse)
                .toList());
    }

    @GetMapping("/public/{slug}/tournaments")
    public ResponseEntity<List<TournamentSummaryResponse>> getPublicTournaments(@PathVariable String slug) {
        Club club = clubQueryService.findPublicBySlugOrThrow(slug);
        return ResponseEntity.ok(clubQueryService.findTournaments(club.getId()).stream()
                .map(ClubController::toTournamentSummaryResponse)
                .toList());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<ClubRolePermissionsResponse>> getPermissionCatalog() {
        return ResponseEntity.ok(Arrays.stream(ClubRole.values())
                .map(role -> new ClubRolePermissionsResponse(role, role.defaultPermissions()))
                .toList());
    }

    @PostMapping
    public ResponseEntity<ClubPageResponse> create(@RequestBody ClubProfileRequest request, Principal principal) {
        Club club = clubService.create(request.toCommand(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toPageResponse(club));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ClubMembershipResponse>> getMyClubs(Principal principal) {
        return ResponseEntity.ok(clubService.findMyClubs(principal.getName()).stream()
                .map(ClubController::toMembershipResponse)
                .toList());
    }

    @GetMapping("/{clubId}/management")
    public ResponseEntity<ClubManagementResponse> getManagement(@PathVariable UUID clubId, Principal principal) {
        ClubService.ClubManagementData data = clubService.getManagementData(clubId, principal.getName());
        List<ClubMemberResponse> members = data.membership().hasPermission(ClubPermission.VIEW_MEMBERS)
                ? clubService.findMembers(clubId, principal.getName()).stream()
                        .map(ClubController::toMemberResponse)
                        .toList()
                : List.of();

        return ResponseEntity.ok(new ClubManagementResponse(
                toPageResponse(data.club(), data.memberCount()),
                toMembershipResponse(data.club(), data.membership(), data.memberCount()),
                members
        ));
    }

    @GetMapping("/{clubId}/rankings")
    public ResponseEntity<List<ClubRankingResponse>> getManagementRanking(
            @PathVariable UUID clubId,
            @RequestParam(required = false) String gender,
            Principal principal
    ) {
        clubService.getMembership(clubId, principal.getName());
        return ResponseEntity.ok(clubRankingService.findRankingByClubId(clubId, gender).stream()
                .map(ClubController::toRankingResponse)
                .toList());
    }

    @GetMapping("/{clubId}/tournaments")
    public ResponseEntity<List<TournamentSummaryResponse>> getManagementTournaments(
            @PathVariable UUID clubId,
            Principal principal
    ) {
        clubService.getMembership(clubId, principal.getName());
        return ResponseEntity.ok(clubQueryService.findTournaments(clubId).stream()
                .map(ClubController::toTournamentSummaryResponse)
                .toList());
    }

    @PutMapping("/{clubId}/page")
    public ResponseEntity<ClubPageResponse> updatePage(
            @PathVariable UUID clubId,
            @RequestBody ClubProfileRequest request,
            Principal principal
    ) {
        Club club = clubService.updatePage(clubId, request.toCommand(), principal.getName());
        return ResponseEntity.ok(toPageResponse(club));
    }

    @GetMapping("/{clubId}/members")
    public ResponseEntity<List<ClubMemberResponse>> getMembers(@PathVariable UUID clubId, Principal principal) {
        return ResponseEntity.ok(clubService.findMembers(clubId, principal.getName()).stream()
                .map(ClubController::toMemberResponse)
                .toList());
    }

    @PostMapping("/{clubId}/members")
    public ResponseEntity<ClubMemberResponse> addMember(
            @PathVariable UUID clubId,
            @RequestBody AddClubMemberRequest request,
            Principal principal
    ) {
        ClubMemberView member = clubService.addMember(clubId, request.email(), request.role(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMemberResponse(member));
    }

    @PutMapping("/{clubId}/members/{membershipId}/role")
    public ResponseEntity<ClubMemberResponse> updateMemberRole(
            @PathVariable UUID clubId,
            @PathVariable UUID membershipId,
            @RequestBody UpdateClubMemberRoleRequest request,
            Principal principal
    ) {
        ClubMemberView member = clubService.updateMemberRole(
                clubId, membershipId, request.role(), principal.getName());
        return ResponseEntity.ok(toMemberResponse(member));
    }

    @PutMapping("/{clubId}/members/{membershipId}/permissions")
    public ResponseEntity<ClubMemberResponse> updateMemberPermissions(
            @PathVariable UUID clubId,
            @PathVariable UUID membershipId,
            @RequestBody UpdateClubMemberPermissionsRequest request,
            Principal principal
    ) {
        ClubMemberView member = clubService.updateMemberPermissions(
                clubId, membershipId, request.permissions(), principal.getName());
        return ResponseEntity.ok(toMemberResponse(member));
    }

    @DeleteMapping("/{clubId}/members/{membershipId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID clubId,
            @PathVariable UUID membershipId,
            Principal principal
    ) {
        clubService.removeMember(clubId, membershipId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    private ClubResponse toClubResponse(Club club) {
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getSlug(),
                club.getTagline(),
                club.getLogoUrl(),
                club.getPrimaryColor(),
                club.getCity(),
                club.getCountry(),
                clubQueryService.countMembers(club.getId())
        );
    }

    private ClubPageResponse toPageResponse(Club club) {
        return toPageResponse(club, clubQueryService.countMembers(club.getId()));
    }

    private static ClubPageResponse toPageResponse(Club club, long memberCount) {
        return new ClubPageResponse(
                club.getId(),
                club.getName(),
                club.getSlug(),
                club.getTagline(),
                club.getDescription(),
                club.getLogoUrl(),
                club.getBannerUrl(),
                club.getPrimaryColor(),
                club.getSecondaryColor(),
                club.getCity(),
                club.getCountry(),
                club.getContactEmail(),
                club.getPhone(),
                club.getWebsite(),
                club.getInstagramUrl(),
                club.getFacebookUrl(),
                club.getXUrl(),
                club.getFoundedYear(),
                club.isPublicPageEnabled(),
                memberCount,
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }

    private static ClubMembershipResponse toMembershipResponse(ClubMembershipSummary membership) {
        return new ClubMembershipResponse(
                membership.membershipId(),
                membership.clubId(),
                membership.name(),
                membership.slug(),
                membership.logoUrl(),
                membership.primaryColor(),
                membership.city(),
                membership.country(),
                membership.role(),
                membership.permissions(),
                membership.memberCount()
        );
    }

    private static ClubMembershipResponse toMembershipResponse(Club club, ClubMembership membership, long memberCount) {
        return new ClubMembershipResponse(
                membership.getId(),
                club.getId(),
                club.getName(),
                club.getSlug(),
                club.getLogoUrl(),
                club.getPrimaryColor(),
                club.getCity(),
                club.getCountry(),
                membership.getRole(),
                membership.effectivePermissions(),
                memberCount
        );
    }

    private static ClubMemberResponse toMemberResponse(ClubMemberView member) {
        return new ClubMemberResponse(
                member.membershipId(),
                member.memberId(),
                member.email(),
                member.firstName(),
                member.lastName(),
                member.role(),
                member.customPermissions(),
                member.effectivePermissions(),
                member.joinedAt()
        );
    }

    private static ClubRankingResponse toRankingResponse(ClubRankingEntry entry) {
        return new ClubRankingResponse(
                entry.position(),
                entry.personId(),
                entry.firstName(),
                entry.lastName(),
                entry.gender(),
                entry.license(),
                entry.points(),
                entry.victories(),
                entry.tournaments()
        );
    }

    private static TournamentSummaryResponse toTournamentSummaryResponse(TournamentSummary tournament) {
        return new TournamentSummaryResponse(
                tournament.id(),
                tournament.name(),
                tournament.playStartDate(),
                tournament.playEndDate(),
                tournament.startTime(),
                tournament.inscriptionStartDate(),
                tournament.inscriptionEndDate(),
                tournament.surface(),
                tournament.maxPlayers(),
                tournament.location(),
                tournament.status(),
                tournament.professionalTournament()
        );
    }
}
