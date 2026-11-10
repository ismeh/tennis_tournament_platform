package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.ClubProfileCommand;
import com.tfm.tennis_platform.domain.exceptions.DuplicateResourceException;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.models.ClubMembership;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.models.club.ClubMemberView;
import com.tfm.tennis_platform.domain.models.club.ClubMembershipSummary;
import com.tfm.tennis_platform.domain.models.enums.ClubPermission;
import com.tfm.tennis_platform.domain.models.enums.ClubRole;
import com.tfm.tennis_platform.domain.port.out.ClubMembershipRepository;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMembershipRepository membershipRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private ClubSlugger clubSlugger;

    @InjectMocks
    private ClubService clubService;

    private static final String EMAIL = "owner@test.com";
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-00000000aaaa");

    @Test
    void create_persists_club_and_owner_membership() {
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder().id(memberId).email(EMAIL).build();
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(clubSlugger.slugify("Club Norte")).thenReturn("club-norte");
        when(clubRepository.existsBySlug("club-norte")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> {
            Club club = invocation.getArgument(0);
            return club.toBuilder().id(UUID.randomUUID()).build();
        });
        when(membershipRepository.save(any(ClubMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Club created = clubService.create(profile("Club Norte", null, null), EMAIL);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Club Norte");
        assertThat(created.getSlug()).isEqualTo("club-norte");
        assertThat(created.getPrimaryColor()).isEqualTo("#16a34a");
        assertThat(created.getSecondaryColor()).isEqualTo("#0284c7");
        assertThat(created.getCreatedBy()).isEqualTo(memberId);
        verify(membershipRepository).save(argThat(membership ->
                membership.getClubId().equals(created.getId())
                        && membership.getMemberId().equals(memberId)
                        && membership.getRole() == ClubRole.OWNER));
    }

    @Test
    void create_generates_unique_slug_when_requested_slug_is_taken() {
        Member member = Member.builder().id(UUID.randomUUID()).email(EMAIL).build();
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(clubSlugger.slugify("club-norte")).thenReturn("club-norte");
        when(clubRepository.existsBySlug("club-norte")).thenReturn(true);
        when(clubSlugger.generateUnique(eq("club-norte"), any())).thenReturn("club-norte-2");
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> {
            Club club = invocation.getArgument(0);
            return club.toBuilder().id(UUID.randomUUID()).build();
        });

        Club created = clubService.create(profile("Club Norte", "club-norte", null), EMAIL);

        assertThat(created.getSlug()).isEqualTo("club-norte-2");
    }

    @Test
    void create_rejects_unknown_creator() {
        when(memberRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.create(profile("Club", null, null), "missing@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(clubRepository, never()).save(any());
    }

    @Test
    void create_requires_a_name() {
        Member member = Member.builder().id(UUID.randomUUID()).email(EMAIL).build();
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> clubService.create(profile("   ", null, null), EMAIL))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void update_page_updates_profile_fields_when_permitted() {
        UUID clubId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).name("Club Norte").slug("club-norte").build();
        ClubMembership membership = membership(clubId, ClubRole.OWNER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, membership.getMemberId()))
                .thenReturn(Optional.of(membership));
        when(clubRepository.findBySlug("nuevo-club")).thenReturn(Optional.empty());
        when(clubSlugger.slugify("nuevo-club")).thenReturn("nuevo-club");
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Club updated = clubService.updatePage(
                clubId,
                profile("Nuevo Club", "nuevo-club", null),
                EMAIL
        );

        assertThat(updated.getName()).isEqualTo("Nuevo Club");
        assertThat(updated.getSlug()).isEqualTo("nuevo-club");
        assertThat(updated.getTagline()).isEqualTo("Tagline");
        assertThat(updated.getPrimaryColor()).isEqualTo("#16a34a");
    }

    @Test
    void update_page_rejects_invalid_color() {
        UUID clubId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).name("Club Norte").slug("club-norte").build();
        ClubMembership membership = membership(clubId, ClubRole.OWNER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, OWNER_ID))
                .thenReturn(Optional.of(membership));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> clubService.updatePage(
                clubId,
                new ClubProfileCommand(null, null, null, null, null, null, "red", null,
                        null, null, null, null, null, null, null, null, null, null),
                EMAIL
        )).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void update_page_rejects_members_without_permission() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.MEMBER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, membership.getMemberId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> clubService.updatePage(clubId, profile("Club", null, null), EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void find_members_returns_member_views_with_person_data() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.ADMIN);
        Person person = Person.builder()
                .id(membership.getPersonId())
                .firstName("Ana")
                .lastName("Lopez")
                .build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, membership.getMemberId()))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.findByClubId(clubId)).thenReturn(List.of(membership));
        when(personRepository.findById(membership.getPersonId())).thenReturn(Optional.of(person));

        List<ClubMemberView> members = clubService.findMembers(clubId, EMAIL);

        assertThat(members).hasSize(1);
        assertThat(members.getFirst().firstName()).isEqualTo("Ana");
        assertThat(members.getFirst().lastName()).isEqualTo("Lopez");
        assertThat(members.getFirst().role()).isEqualTo(ClubRole.ADMIN);
    }

    @Test
    void add_member_adds_default_member_role() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        Member target = Member.builder().id(UUID.randomUUID()).email("new@test.com").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.of(target));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(Club.builder().id(clubId).build()));
        when(membershipRepository.existsByClubIdAndMemberId(clubId, target.getId())).thenReturn(false);
        when(membershipRepository.save(any(ClubMembership.class))).thenAnswer(invocation ->
                ((ClubMembership) invocation.getArgument(0)).toBuilder()
                        .email("new@test.com")
                        .build());

        ClubMemberView view = clubService.addMember(clubId, " New@Test.com ", null, EMAIL);

        assertThat(view.email()).isEqualTo("new@test.com");
        assertThat(view.role()).isEqualTo(ClubRole.MEMBER);
        assertThat(view.effectivePermissions()).contains(ClubPermission.VIEW_RANKINGS);
    }

    @Test
    void add_member_rejects_existing_membership() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        Member target = Member.builder().id(UUID.randomUUID()).email("new@test.com").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.of(target));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(Club.builder().id(clubId).build()));
        when(membershipRepository.existsByClubIdAndMemberId(clubId, target.getId())).thenReturn(true);

        assertThatThrownBy(() -> clubService.addMember(clubId, "new@test.com", ClubRole.MEMBER, EMAIL))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void add_member_rejects_owner_role() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        Member target = Member.builder().id(UUID.randomUUID()).email("new@test.com").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.of(target));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(Club.builder().id(clubId).build()));
        when(membershipRepository.existsByClubIdAndMemberId(clubId, target.getId())).thenReturn(false);

        assertThatThrownBy(() -> clubService.addMember(clubId, "new@test.com", ClubRole.OWNER, EMAIL))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void add_member_rejects_admin_assignment_by_non_owner() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.ADMIN);
        Member target = Member.builder().id(UUID.randomUUID()).email("new@test.com").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.of(target));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(Club.builder().id(clubId).build()));
        when(membershipRepository.existsByClubIdAndMemberId(clubId, target.getId())).thenReturn(false);

        assertThatThrownBy(() -> clubService.addMember(clubId, "new@test.com", ClubRole.ADMIN, EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_member_role_changes_non_owner_membership() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        ClubMembership target = membership(clubId, ClubRole.MEMBER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(membershipRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(membershipRepository.save(any(ClubMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClubMemberView view = clubService.updateMemberRole(clubId, target.getId(), ClubRole.COACH, EMAIL);

        assertThat(view.role()).isEqualTo(ClubRole.COACH);
    }

    @Test
    void update_member_role_rejects_owner_membership() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        ClubMembership owner = membership(clubId, ClubRole.OWNER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(membershipRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> clubService.updateMemberRole(clubId, owner.getId(), ClubRole.ADMIN, EMAIL))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void update_member_permissions_stores_custom_grants() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        ClubMembership target = membership(clubId, ClubRole.MEMBER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(membershipRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(membershipRepository.save(any(ClubMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClubMemberView view = clubService.updateMemberPermissions(
                clubId,
                target.getId(),
                EnumSet.of(ClubPermission.MANAGE_MEMBERS),
                EMAIL
        );

        assertThat(view.customPermissions()).containsExactly(ClubPermission.MANAGE_MEMBERS);
        assertThat(view.effectivePermissions())
                .contains(ClubPermission.VIEW_RANKINGS, ClubPermission.MANAGE_MEMBERS);
    }

    @Test
    void update_member_permissions_rejects_owner_membership() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        ClubMembership owner = membership(clubId, ClubRole.OWNER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(membershipRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> clubService.updateMemberPermissions(
                clubId, owner.getId(), Set.of(ClubPermission.VIEW_MEMBERS), EMAIL))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void remove_member_deletes_non_owner_membership() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.ADMIN);
        ClubMembership target = membership(clubId, ClubRole.MEMBER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(membershipRepository.findById(target.getId())).thenReturn(Optional.of(target));

        clubService.removeMember(clubId, target.getId(), EMAIL);

        verify(membershipRepository).delete(target.getId());
    }

    @Test
    void remove_member_rejects_owner() {
        UUID clubId = UUID.randomUUID();
        ClubMembership requester = membership(clubId, ClubRole.OWNER);
        ClubMembership owner = membership(clubId, ClubRole.OWNER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, requester.getMemberId()))
                .thenReturn(Optional.of(requester));
        when(membershipRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> clubService.removeMember(clubId, owner.getId(), EMAIL))
                .isInstanceOf(InvalidArgumentException.class);
        verify(membershipRepository, never()).delete(any());
    }

    @Test
    void find_my_clubs_returns_membership_summaries() {
        clubServiceShouldReturnMyClubs();
    }

    private void clubServiceShouldReturnMyClubs() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.ADMIN);
        Club club = Club.builder()
                .id(clubId)
                .name("Club Norte")
                .slug("club-norte")
                .primaryColor("#16a34a")
                .build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByMemberId(membership.getMemberId())).thenReturn(List.of(membership));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(membershipRepository.countByClubId(clubId)).thenReturn(4L);

        List<ClubMembershipSummary> result = clubService.findMyClubs(EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Club Norte");
        assertThat(result.getFirst().role()).isEqualTo(ClubRole.ADMIN);
        assertThat(result.getFirst().memberCount()).isEqualTo(4L);
    }

    @Test
    void get_management_data_returns_club_membership_and_count() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.ADMIN);
        Club club = Club.builder().id(clubId).name("Club Norte").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, membership.getMemberId()))
                .thenReturn(Optional.of(membership));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(membershipRepository.countByClubId(clubId)).thenReturn(3L);

        ClubService.ClubManagementData data = clubService.getManagementData(clubId, EMAIL);

        assertThat(data.club()).isEqualTo(club);
        assertThat(data.membership()).isEqualTo(membership);
        assertThat(data.memberCount()).isEqualTo(3L);
    }

    @Test
    void member_can_view_own_membership() {
        UUID clubId = UUID.randomUUID();
        ClubMembership membership = membership(clubId, ClubRole.MEMBER);

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, membership.getMemberId()))
                .thenReturn(Optional.of(membership));

        assertThat(clubService.getMembership(clubId, EMAIL)).isEqualTo(membership);
    }

    @Test
    void non_member_cannot_access_club_management() {
        UUID clubId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(
                Member.builder().id(memberId).email(EMAIL).build()));
        when(membershipRepository.findByClubIdAndMemberId(clubId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.getMembership(clubId, EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Member member() {
        return Member.builder().id(OWNER_ID).email(EMAIL).build();
    }

    private ClubMembership membership(UUID clubId, ClubRole role) {
        return ClubMembership.builder()
                .id(UUID.randomUUID())
                .clubId(clubId)
                .memberId(OWNER_ID)
                .personId(UUID.randomUUID())
                .email("member@test.com")
                .role(role)
                .customPermissions(EnumSet.noneOf(ClubPermission.class))
                .build();
    }

    private ClubProfileCommand profile(String name, String slug, String tagline) {
        return new ClubProfileCommand(
                name,
                slug,
                tagline != null ? tagline : "Tagline",
                "Descripción del club",
                "https://example.com/logo.png",
                "https://example.com/banner.png",
                null,
                null,
                "Madrid",
                "España",
                "contacto@club.com",
                "+34 600 000 000",
                "https://club.com",
                "https://instagram.com/club",
                "https://facebook.com/club",
                "https://x.com/club",
                1990,
                true
        );
    }
}
