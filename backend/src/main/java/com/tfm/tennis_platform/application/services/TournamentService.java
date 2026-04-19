package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Tournament create(Tournament tournament, String creatorEmail) {
        Member creator = memberRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with email: " + creatorEmail));

        Tournament tournamentToSave = Tournament.builder()
                .id(UUID.randomUUID())
                .name(tournament.getName())
                .playPeriod(new TournamentPeriod(
                        tournament.getPlayPeriod().startDate(),
                        tournament.getPlayPeriod().endDate()))
                .inscriptionPeriod(new TournamentPeriod(
                        tournament.getInscriptionPeriod().startDate(),
                        tournament.getInscriptionPeriod().endDate()))
                .surface(tournament.getSurface())
                .maxPlayers(tournament.getMaxPlayers())
                .location(tournament.getLocation())
                .createdBy(creator)
                .build();

        return tournamentRepository.save(tournamentToSave);
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id);
    }

    @Transactional
    public Tournament updateStatus(UUID tournamentId, TournamentStatus newStatus) {
        Tournament currentTournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (newStatus == null) {
            throw new IllegalArgumentException("Tournament status must not be null");
        }

        TournamentStatus currentStatus = currentTournament.getState();
        if (currentStatus == newStatus) {
            return currentTournament;
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Invalid status transition: " + currentStatus + " -> " + newStatus);
        }

        Tournament updatedTournament = Tournament.builder()
                .id(currentTournament.getId())
                .name(currentTournament.getName())
                .playPeriod(currentTournament.getPlayPeriod())
                .inscriptionPeriod(currentTournament.getInscriptionPeriod())
                .surface(currentTournament.getSurface())
                .maxPlayers(currentTournament.getMaxPlayers())
                .location(currentTournament.getLocation())
                .state(newStatus)
                .createdBy(currentTournament.getCreatedBy())
                .events(currentTournament.getEvents())
                .build();

        return tournamentRepository.save(updatedTournament);
    }
}
