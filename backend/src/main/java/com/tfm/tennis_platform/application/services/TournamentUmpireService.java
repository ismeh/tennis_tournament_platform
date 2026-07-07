package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.domain.models.UmpireSearchResult;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentUmpireRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentUmpireService {

    private final TournamentUmpireRepository tournamentUmpireRepository;
    private final TournamentRepository tournamentRepository;
    private final MemberRepository memberRepository;
    private final TournamentService tournamentService;

    @Transactional
    public TournamentUmpire addUmpire(UUID tournamentId, UUID umpireId, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        Member umpire = memberRepository.findById(umpireId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", umpireId));

        if (umpire.getRole() != UserRole.UMPIRE && umpire.getRole() != UserRole.ORGANIZER) {
            throw new InvalidArgumentException("El usuario seleccionado no tiene un rol válido para ser árbitro (debe ser Árbitro u Organizador).");
        }

        if (tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId)) {
            throw new InvalidArgumentException("Este árbitro ya está asignado al torneo.");
        }

        TournamentUmpire tournamentUmpire = TournamentUmpire.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .umpireId(umpireId)
                .assignedAt(LocalDateTime.now())
                .build();

        return tournamentUmpireRepository.save(tournamentUmpire);
    }

    @Transactional
    public void removeUmpire(UUID tournamentId, UUID umpireId, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        if (!tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId)) {
            throw new ResourceNotFoundException("TournamentUmpire", umpireId);
        }

        tournamentUmpireRepository.deleteByTournamentIdAndUmpireId(tournamentId, umpireId);
    }

    public List<TournamentUmpire> findByTournamentId(UUID tournamentId) {
        return tournamentUmpireRepository.findByTournamentId(tournamentId);
    }

    public boolean isUmpireAssignedToTournament(UUID tournamentId, UUID memberId) {
        return tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, memberId);
    }

    public List<UmpireSearchResult> searchUmpires(String query) {
        return memberRepository.searchUmpiresWithPersonData(query);
    }

    public List<UmpireSearchResult> searchByRoles(List<UserRole> roles, String query) {
        return memberRepository.searchByRolesWithPersonData(roles, query);
    }
}
