package com.puntomatch.application.services;

import com.puntomatch.domain.exceptions.DuplicateResourceException;
import com.puntomatch.domain.exceptions.InvalidArgumentException;
import com.puntomatch.domain.exceptions.ResourceNotFoundException;
import com.puntomatch.domain.models.Court;
import com.puntomatch.domain.port.out.CourtRepository;
import com.puntomatch.domain.port.out.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final TournamentRepository tournamentRepository;

    public List<Court> findByTournamentId(UUID tournamentId) {
        ensureTournamentExists(tournamentId);
        return courtRepository.findByTournamentId(tournamentId);
    }

    public Court create(UUID tournamentId, String name) {
        ensureTournamentExists(tournamentId);
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

    public Court update(UUID tournamentId, UUID courtId, String name) {
        ensureTournamentExists(tournamentId);
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

    public void delete(UUID tournamentId, UUID courtId) {
        ensureTournamentExists(tournamentId);
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

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidArgumentException("El nombre de la pista es obligatorio.");
        }

        return name.trim();
    }
}
