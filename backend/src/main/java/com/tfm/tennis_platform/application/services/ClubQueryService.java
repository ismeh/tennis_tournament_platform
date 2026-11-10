package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.port.out.ClubMembershipRepository;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubQueryService {

    private final ClubRepository clubRepository;
    private final ClubMembershipRepository membershipRepository;
    private final TournamentRepository tournamentRepository;
    private final ClubSlugger clubSlugger;

    @Transactional(readOnly = true)
    public List<Club> search(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return clubRepository.findByNameContaining(query.trim());
    }

    @Transactional(readOnly = true)
    public List<Club> findPublicClubs(String query) {
        String normalized = query == null ? null : query.trim();
        if (normalized != null && normalized.length() < 2) {
            return List.of();
        }
        return clubRepository.findPublicClubs(normalized);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findPublicBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return clubRepository.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .filter(Club::isPublicPageEnabled);
    }

    @Transactional(readOnly = true)
    public Club findPublicBySlugOrThrow(String slug) {
        return findPublicBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la página pública del club solicitado."));
    }

    @Transactional(readOnly = true)
    public Optional<Club> findById(UUID id) {
        return clubRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Club findByIdOrThrow(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el club solicitado."));
    }

    @Transactional(readOnly = true)
    public long countMembers(UUID clubId) {
        return membershipRepository.countByClubId(clubId);
    }

    @Transactional(readOnly = true)
    public List<TournamentSummary> findTournaments(UUID clubId) {
        return tournamentRepository.findSummariesByClubId(clubId);
    }

    @Transactional
    public Club findOrCreate(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        return clubRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> clubRepository.save(Club.builder()
                        .name(trimmed)
                        .slug(clubSlugger.generateUnique(trimmed, clubRepository::existsBySlug))
                        .publicPageEnabled(true)
                        .build()));
    }
}
