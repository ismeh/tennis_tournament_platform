package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
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
    private final CourtRepository courtRepository;

    @Transactional
    public Tournament create(Tournament tournament, String creatorEmail, Integer courtCount) {
        Member creator = memberRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member", creatorEmail));

        if (tournament.getStartTime() == null) {
            throw new InvalidArgumentException("La hora de inicio del torneo es obligatoria.");
        }

        Tournament tournamentToSave = Tournament.builder()
                .id(UUID.randomUUID())
                .name(tournament.getName())
                .playPeriod(new TournamentPeriod(
                        tournament.getPlayPeriod().startDate(),
                        tournament.getPlayPeriod().endDate()))
                .startTime(tournament.getStartTime())
                .inscriptionPeriod(new TournamentPeriod(
                        tournament.getInscriptionPeriod().startDate(),
                        tournament.getInscriptionPeriod().endDate()))
                .surface(tournament.getSurface())
                .maxPlayers(tournament.getMaxPlayers())
                .location(tournament.getLocation())
                .createdBy(creator)
                .build();

        Tournament savedTournament = tournamentRepository.save(tournamentToSave);
        createInitialCourts(savedTournament.getId(), courtCount);
        return savedTournament;
    }

    public Tournament create(Tournament tournament, String creatorEmail) {
        return create(tournament, creatorEmail, 0);
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public List<TournamentSummary> findSummaries() {
        return tournamentRepository.findSummaries();
    }

    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id);
    }

    public boolean isProfessionalTournament(UUID id) {
        return tournamentRepository.isProfessionalTournament(id);
    }

    @Transactional
    public Tournament updateStatus(UUID tournamentId, TournamentStatus newStatus) {
        Tournament currentTournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        if (newStatus == null) {
            throw new InvalidArgumentException("Selecciona un estado válido para el torneo.");
        }

        TournamentStatus currentStatus = currentTournament.getState();
        if (currentStatus == newStatus) {
            return currentTournament;
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidArgumentException("No se puede cambiar el torneo de " + currentStatus + " a " + newStatus + ".");
        }

        Tournament updatedTournament = currentTournament.toBuilder()
                .state(newStatus)
                .build();

        return tournamentRepository.save(updatedTournament);
    }

    private void createInitialCourts(UUID tournamentId, Integer courtCount) {
        if (courtCount == null || courtCount == 0) {
            return;
        }
        if (courtCount < 0) {
            throw new InvalidArgumentException("El número de pistas no puede ser negativo.");
        }

        for (int courtNumber = 1; courtNumber <= courtCount; courtNumber++) {
            String courtName = "Pista " + courtNumber;
            if (courtRepository.existsByTournamentIdAndName(tournamentId, courtName)) {
                continue;
            }

            courtRepository.save(Court.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournamentId)
                    .name(courtName)
                    .active(true)
                    .build());
        }
    }
}
