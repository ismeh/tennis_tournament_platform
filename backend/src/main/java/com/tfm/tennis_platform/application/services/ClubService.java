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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ClubService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String DEFAULT_PRIMARY_COLOR = "#16a34a";
    private static final String DEFAULT_SECONDARY_COLOR = "#0284c7";

    private final ClubRepository clubRepository;
    private final ClubMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final PersonRepository personRepository;
    private final ClubSlugger clubSlugger;

    @Transactional
    public Club create(ClubProfileCommand command, String creatorEmail) {
        Member creator = requireMember(creatorEmail);
        String name = normalizeRequiredName(command.name());
        String slug = resolveSlug(command.slug(), name, null);

        Club club = Club.builder()
                .name(name)
                .slug(slug)
                .publicPageEnabled(command.publicPageEnabled() == null || command.publicPageEnabled())
                .createdBy(creator.getId())
                .build();

        Club saved = clubRepository.save(applyProfile(club, command, false));

        membershipRepository.save(ClubMembership.builder()
                .clubId(saved.getId())
                .memberId(creator.getId())
                .role(ClubRole.OWNER)
                .customPermissions(EnumSet.noneOf(ClubPermission.class))
                .build());

        return saved;
    }

    @Transactional(readOnly = true)
    public ClubManagementData getManagementData(UUID clubId, String requesterEmail) {
        ClubMembership membership = requireMembership(clubId, requesterEmail);
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el club solicitado."));
        return new ClubManagementData(club, membership, membershipRepository.countByClubId(clubId));
    }

    @Transactional(readOnly = true)
    public ClubMembership getMembership(UUID clubId, String requesterEmail) {
        return requireMembership(clubId, requesterEmail);
    }

    @Transactional(readOnly = true)
    public List<ClubMembershipSummary> findMyClubs(String email) {
        Member member = requireMember(email);
        return membershipRepository.findByMemberId(member.getId()).stream()
                .map(membership -> clubRepository.findById(membership.getClubId())
                        .map(club -> new ClubMembershipSummary(
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
                                membershipRepository.countByClubId(club.getId())
                        ))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public Club updatePage(UUID clubId, ClubProfileCommand command, String requesterEmail) {
        requirePermission(clubId, requesterEmail, ClubPermission.EDIT_CLUB_PAGE);
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el club solicitado."));
        String name = command.name() == null || command.name().isBlank()
                ? club.getName()
                : normalizeRequiredName(command.name());
        String slug = command.slug() == null || command.slug().isBlank()
                ? club.getSlug()
                : resolveSlug(command.slug(), name, clubId);

        Club updated = applyProfile(club.toBuilder()
                .name(name)
                .slug(slug)
                .build(), command, true);

        return clubRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public List<ClubMemberView> findMembers(UUID clubId, String requesterEmail) {
        requirePermission(clubId, requesterEmail, ClubPermission.VIEW_MEMBERS);
        return membershipRepository.findByClubId(clubId).stream()
                .map(this::toMemberView)
                .toList();
    }

    @Transactional
    public ClubMemberView addMember(UUID clubId, String email, ClubRole requestedRole, String requesterEmail) {
        ClubMembership requester = requirePermission(clubId, requesterEmail, ClubPermission.MANAGE_MEMBERS);
        Member target = memberRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna cuenta con ese email."));

        clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el club solicitado."));

        if (membershipRepository.existsByClubIdAndMemberId(clubId, target.getId())) {
            throw new DuplicateResourceException("Esa cuenta ya pertenece al club.");
        }

        ClubRole role = requestedRole != null ? requestedRole : ClubRole.MEMBER;
        if (role == ClubRole.OWNER) {
            throw new InvalidArgumentException("Un club solo puede tener un propietario.");
        }
        if (role != ClubRole.MEMBER && !requester.hasPermission(ClubPermission.MANAGE_PERMISSIONS)) {
            throw new AccessDeniedException("Solo el propietario puede asignar roles de administración.");
        }

        ClubMembership saved = membershipRepository.save(ClubMembership.builder()
                .clubId(clubId)
                .memberId(target.getId())
                .role(role)
                .customPermissions(EnumSet.noneOf(ClubPermission.class))
                .build());

        return toMemberView(saved);
    }

    @Transactional
    public ClubMemberView updateMemberRole(UUID clubId, UUID membershipId, ClubRole role, String requesterEmail) {
        requirePermission(clubId, requesterEmail, ClubPermission.MANAGE_PERMISSIONS);
        if (role == null) {
            throw new InvalidArgumentException("Selecciona un rol válido.");
        }
        if (role == ClubRole.OWNER) {
            throw new InvalidArgumentException("No puedes transferir la propiedad del club desde este panel.");
        }

        ClubMembership target = findMembershipInClub(clubId, membershipId);
        if (target.getRole() == ClubRole.OWNER) {
            throw new InvalidArgumentException("No se puede cambiar el rol del propietario del club.");
        }

        ClubMembership updated = membershipRepository.save(target.toBuilder().role(role).build());
        return toMemberView(updated);
    }

    @Transactional
    public ClubMemberView updateMemberPermissions(
            UUID clubId,
            UUID membershipId,
            Set<ClubPermission> customPermissions,
            String requesterEmail
    ) {
        requirePermission(clubId, requesterEmail, ClubPermission.MANAGE_PERMISSIONS);
        ClubMembership target = findMembershipInClub(clubId, membershipId);
        if (target.getRole() == ClubRole.OWNER) {
            throw new InvalidArgumentException("El propietario ya tiene todos los permisos del club.");
        }

        Set<ClubPermission> permissions = customPermissions == null || customPermissions.isEmpty()
                ? EnumSet.noneOf(ClubPermission.class)
                : EnumSet.copyOf(customPermissions);

        ClubMembership updated = membershipRepository.save(target.toBuilder()
                .customPermissions(permissions)
                .build());
        return toMemberView(updated);
    }

    @Transactional
    public void removeMember(UUID clubId, UUID membershipId, String requesterEmail) {
        requirePermission(clubId, requesterEmail, ClubPermission.MANAGE_MEMBERS);
        ClubMembership target = findMembershipInClub(clubId, membershipId);
        if (target.getRole() == ClubRole.OWNER) {
            throw new InvalidArgumentException("No puedes eliminar al propietario del club.");
        }
        membershipRepository.delete(target.getId());
    }

    private Club applyProfile(Club club, ClubProfileCommand command, boolean allowNullName) {
        Club.ClubBuilder builder = club.toBuilder();

        if (command.name() != null && !command.name().isBlank()) {
            builder.name(normalizeRequiredName(command.name()));
        } else if (!allowNullName && (club.getName() == null || club.getName().isBlank())) {
            throw new InvalidArgumentException("El nombre del club es obligatorio.");
        }

        if (command.tagline() != null) {
            builder.tagline(normalizeOptional(command.tagline(), 180, "El eslogan no puede superar los 180 caracteres."));
        }
        if (command.description() != null) {
            builder.description(normalizeOptional(command.description(), 5000, "La descripción no puede superar los 5000 caracteres."));
        }
        if (command.logoUrl() != null) {
            builder.logoUrl(normalizeOptional(command.logoUrl(), 500, "La URL del logo no es válida."));
        }
        if (command.bannerUrl() != null) {
            builder.bannerUrl(normalizeOptional(command.bannerUrl(), 500, "La URL de la portada no es válida."));
        }
        if (command.primaryColor() != null) {
            builder.primaryColor(normalizeColor(command.primaryColor(), DEFAULT_PRIMARY_COLOR));
        }
        if (command.secondaryColor() != null) {
            builder.secondaryColor(normalizeColor(command.secondaryColor(), DEFAULT_SECONDARY_COLOR));
        }
        if (command.city() != null) {
            builder.city(normalizeOptional(command.city(), 120, "La ciudad no puede superar los 120 caracteres."));
        }
        if (command.country() != null) {
            builder.country(normalizeOptional(command.country(), 120, "El país no puede superar los 120 caracteres."));
        }
        if (command.contactEmail() != null) {
            String contactEmail = normalizeOptional(command.contactEmail(), 180, "El email de contacto no es válido.");
            if (contactEmail != null && !EMAIL.matcher(contactEmail).matches()) {
                throw new InvalidArgumentException("El email de contacto no es válido.");
            }
            builder.contactEmail(contactEmail);
        }
        if (command.phone() != null) {
            builder.phone(normalizeOptional(command.phone(), 40, "El teléfono no puede superar los 40 caracteres."));
        }
        if (command.website() != null) {
            builder.website(normalizeOptional(command.website(), 300, "La web no puede superar los 300 caracteres."));
        }
        if (command.instagramUrl() != null) {
            builder.instagramUrl(normalizeOptional(command.instagramUrl(), 300, "La URL de Instagram no es válida."));
        }
        if (command.facebookUrl() != null) {
            builder.facebookUrl(normalizeOptional(command.facebookUrl(), 300, "La URL de Facebook no es válida."));
        }
        if (command.xUrl() != null) {
            builder.xUrl(normalizeOptional(command.xUrl(), 300, "La URL de X no es válida."));
        }
        if (command.foundedYear() != null) {
            int maxYear = Year.now().getValue() + 1;
            if (command.foundedYear() < 1900 || command.foundedYear() > maxYear) {
                throw new InvalidArgumentException("El año de fundación no es válido.");
            }
            builder.foundedYear(command.foundedYear());
        }
        if (command.publicPageEnabled() != null) {
            builder.publicPageEnabled(command.publicPageEnabled());
        }

        if (club.getPrimaryColor() == null && command.primaryColor() == null) {
            builder.primaryColor(DEFAULT_PRIMARY_COLOR);
        }
        if (club.getSecondaryColor() == null && command.secondaryColor() == null) {
            builder.secondaryColor(DEFAULT_SECONDARY_COLOR);
        }

        return builder.build();
    }

    private ClubMembership requirePermission(UUID clubId, String email, ClubPermission permission) {
        ClubMembership membership = requireMembership(clubId, email);
        if (!membership.hasPermission(permission)) {
            throw new AccessDeniedException("No tienes permisos para realizar esta acción en el club.");
        }
        return membership;
    }

    private ClubMembership requireMembership(UUID clubId, String email) {
        Member member = requireMember(email);
        return membershipRepository.findByClubIdAndMemberId(clubId, member.getId())
                .orElseThrow(() -> new AccessDeniedException("No perteneces a este club."));
    }

    private Member requireMember(String email) {
        return memberRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cuenta solicitada."));
    }

    private ClubMembership findMembershipInClub(UUID clubId, UUID membershipId) {
        ClubMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el miembro solicitado."));
        if (!clubId.equals(membership.getClubId())) {
            throw new ResourceNotFoundException("No se encontró el miembro solicitado.");
        }
        return membership;
    }

    private ClubMemberView toMemberView(ClubMembership membership) {
        Person person = membership.getPersonId() != null
                ? personRepository.findById(membership.getPersonId()).orElse(null)
                : null;

        String email = membership.getEmail();
        String firstName = person != null ? person.getFirstName() : null;
        String lastName = person != null ? person.getLastName() : null;
        if (firstName == null && email != null) {
            firstName = email.split("@")[0];
        }

        return new ClubMemberView(
                membership.getId(),
                membership.getMemberId(),
                email,
                firstName,
                lastName,
                membership.getRole(),
                membership.getCustomPermissions(),
                membership.effectivePermissions(),
                membership.getJoinedAt()
        );
    }

    private String resolveSlug(String requestedSlug, String name, UUID currentClubId) {
        String slug = requestedSlug != null && !requestedSlug.isBlank()
                ? clubSlugger.slugify(requestedSlug)
                : clubSlugger.slugify(name);

        if (slug.isBlank()) {
            throw new InvalidArgumentException("La dirección pública del club no es válida.");
        }

        boolean taken = currentClubId == null
                ? clubRepository.existsBySlug(slug)
                : clubRepository.findBySlug(slug)
                        .filter(existing -> !existing.getId().equals(currentClubId))
                        .isPresent();

        if (taken) {
            if (currentClubId != null) {
                throw new DuplicateResourceException("Ya existe otro club con esa dirección pública.");
            }
            slug = clubSlugger.generateUnique(slug, clubRepository::existsBySlug);
        }

        return slug;
    }

    private String normalizeRequiredName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidArgumentException("El nombre del club es obligatorio.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 150) {
            throw new InvalidArgumentException("El nombre del club no puede superar los 150 caracteres.");
        }
        return trimmed;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidArgumentException("Indica un email válido.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new InvalidArgumentException("Indica un email válido.");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength, String errorMessage) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new InvalidArgumentException(errorMessage);
        }
        return trimmed;
    }

    private String normalizeColor(String color, String fallback) {
        String value = color == null ? null : color.trim();
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        if (!HEX_COLOR.matcher(value).matches()) {
            throw new InvalidArgumentException("Los colores del club deben usar formato hexadecimal, por ejemplo #16a34a.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public record ClubManagementData(Club club, ClubMembership membership, long memberCount) {
    }
}
