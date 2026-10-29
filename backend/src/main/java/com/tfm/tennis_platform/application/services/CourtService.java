package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.DuplicateResourceException;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentService tournamentService;

    public List<Court> findByTournamentId(UUID tournamentId) {
        ensureTournamentExists(tournamentId);
        return courtRepository.findByTournamentId(tournamentId);
    }

    public Court create(UUID tournamentId, String name, String requesterEmail) {
        ensureTournamentAdmin(tournamentId, requesterEmail);
        validateTournamentCanModifyCourts(tournamentId);
        String normalizedName = normalizeName(name);

        if (courtRepository.existsByTournamentIdAndName(tournamentId, normalizedName)) {
            throw new DuplicateResourceException("Ya existe una pista con ese nombre en el torneo.");
        }

        Court court = Court.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .name(normalizedName)
                .active(true)
                .build();

        return courtRepository.save(court);
    }

    public Court update(UUID tournamentId, UUID courtId, String name, String requesterEmail) {
        ensureTournamentAdmin(tournamentId, requesterEmail);
        validateTournamentCanModifyCourts(tournamentId);
        if (courtId == null) {
            throw new InvalidArgumentException("La pista es obligatoria.");
        }

        Court currentCourt = courtRepository.findByIdAndTournamentId(courtId, tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", courtId));
        String normalizedName = normalizeName(name);

        if (!currentCourt.getName().equalsIgnoreCase(normalizedName)
                && courtRepository.existsByTournamentIdAndName(tournamentId, normalizedName)) {
            throw new DuplicateResourceException("Ya existe una pista con ese nombre en el torneo.");
        }

        return courtRepository.save(currentCourt.toBuilder()
                .name(normalizedName)
                .build());
    }

    public void delete(UUID tournamentId, UUID courtId, String requesterEmail) {
        ensureTournamentAdmin(tournamentId, requesterEmail);
        validateTournamentCanModifyCourts(tournamentId);
        if (courtId == null) {
            throw new InvalidArgumentException("La pista es obligatoria.");
        }

        Court currentCourt = courtRepository.findByIdAndTournamentId(courtId, tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", courtId));
        courtRepository.deleteById(currentCourt.getId());
    }

    private void ensureTournamentExists(UUID tournamentId) {
        if (tournamentId == null) {
            throw new InvalidArgumentException("El torneo es obligatorio.");
        }

        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
    }

    private void ensureTournamentAdmin(UUID tournamentId, String requesterEmail) {
        ensureTournamentExists(tournamentId);
        tournamentService.assertTournamentAdmin(tournamentId, requesterEmail);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidArgumentException("El nombre de la pista es obligatorio.");
        }

        return name.trim();
    }

    private void validateTournamentCanModifyCourts(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        TournamentStatus state = tournament.getState();
        if (state == TournamentStatus.CLOSED || state == TournamentStatus.IN_PROGRESS
                || state == TournamentStatus.COMPLETED || state == TournamentStatus.CANCELLED) {
            throw new InvalidArgumentException(
                    "No se pueden modificar pistas una vez el torneo está cerrado o en curso. Estado actual: " + state);
        }
    }
}
