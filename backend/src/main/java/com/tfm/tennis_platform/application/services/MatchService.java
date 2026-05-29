package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final CourtRepository courtRepository;

    public Match update(Match match) {
        return matchRepository.save(match);
    }

    @Transactional
    public Match recordResult(UUID tournamentId, UUID matchId, UUID winnerId, String scoreString) {
        if (tournamentId == null) {
            throw new InvalidArgumentException("El torneo es obligatorio.");
        }
        if (matchId == null) {
            throw new InvalidArgumentException("El partido es obligatorio.");
        }
        if (winnerId == null) {
            throw new InvalidArgumentException("Selecciona el ganador del partido.");
        }
        if (scoreString == null || scoreString.isBlank()) {
            throw new InvalidArgumentException("Indica el resultado del partido.");
        }

        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        Match currentMatch = matchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> matchId.equals(match.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));

        if (!winnerId.equals(currentMatch.getFirstInscriptionId()) && !winnerId.equals(currentMatch.getSecondInscriptionId())) {
            throw new InvalidArgumentException("El ganador debe ser uno de los participantes del partido.");
        }

        Match updatedCurrentMatch = currentMatch.toBuilder()
                .winner(createInscriptionReference(winnerId))
                .result(scoreString.trim())
                .build();

        Match savedCurrentMatch = matchRepository.save(updatedCurrentMatch);

        if (currentMatch.getNextMatch() != null && currentMatch.getNextMatch().getId() != null) {
            Match nextMatch = matchRepository.findById(currentMatch.getNextMatch().getId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Next match", currentMatch.getNextMatch().getId()));

            Match updatedNextMatch = placeWinnerInNextMatch(nextMatch, winnerId);
            matchRepository.save(updatedNextMatch);
        }

        return savedCurrentMatch;
    }

    public List<Match> findByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId);
    }

    public Optional<Match> findById(String id) {
        return matchRepository.findById(id);
    }

    @Transactional
    public Match schedule(UUID tournamentId, UUID matchId, UUID courtId, LocalDateTime scheduledAt, ScheduleTimeType scheduleTimeType) {
        if (tournamentId == null) {
            throw new InvalidArgumentException("El torneo es obligatorio.");
        }
        if (matchId == null) {
            throw new InvalidArgumentException("El partido es obligatorio.");
        }
        if (courtId == null) {
            throw new InvalidArgumentException("Selecciona una pista para el partido.");
        }
        if (scheduledAt == null) {
            throw new InvalidArgumentException("Indica la fecha y hora del partido.");
        }

        ScheduleTimeType resolvedType = scheduleTimeType != null ? scheduleTimeType : ScheduleTimeType.EXACT;
        var tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        if (scheduledAt.toLocalDate().isBefore(tournament.getPlayPeriod().startDate())
                || scheduledAt.toLocalDate().isAfter(tournament.getPlayPeriod().endDate())) {
            throw new InvalidArgumentException("La hora del partido debe estar dentro del periodo de juego del torneo.");
        }

        Court court = courtRepository.findByIdAndTournamentId(courtId, tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", courtId));

        if (!court.isActive()) {
            throw new InvalidArgumentException("La pista seleccionada no está activa.");
        }

        List<Match> tournamentMatches = matchRepository.findByTournamentId(tournamentId);
        Match currentMatch = tournamentMatches.stream()
                .filter(match -> matchId.equals(match.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));

        boolean courtBusy = tournamentMatches.stream()
                .filter(match -> !matchId.equals(match.getId()))
                .anyMatch(match -> isSameCourtSlot(match, courtId, scheduledAt));

        if (courtBusy) {
            throw new InvalidArgumentException("La pista ya está ocupada en esa hora.");
        }

        Match updatedMatch = currentMatch.toBuilder()
                .scheduledAt(scheduledAt)
                .scheduleTimeType(resolvedType)
                .courtId(court.getId())
                .court(court.getName())
                .build();

        return matchRepository.save(updatedMatch);
    }

    private Match placeWinnerInNextMatch(Match nextMatch, UUID winnerId) {
        if (winnerId.equals(nextMatch.getFirstInscriptionId()) || winnerId.equals(nextMatch.getSecondInscriptionId())) {
            return nextMatch;
        }

        if (nextMatch.getFirstInscriptionId() == null) {
            return nextMatch.toBuilder()
                    .firstInscription(createInscriptionReference(winnerId))
                    .build();
        }

        if (nextMatch.getSecondInscriptionId() == null) {
            return nextMatch.toBuilder()
                    .secondInscription(createInscriptionReference(winnerId))
                    .build();
        }

        throw new InvalidArgumentException("El siguiente partido ya tiene los dos participantes asignados.");
    }

    private Inscription createInscriptionReference(UUID inscriptionId) {
        return Inscription.builder()
                .id(inscriptionId)
                .build();
    }

    private boolean isSameCourtSlot(Match match, UUID courtId, LocalDateTime scheduledAt) {
        return courtId.equals(match.getCourtId()) && scheduledAt.equals(match.getScheduledAt());
    }
}
