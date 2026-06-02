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

        UUID loserId = resolveLoserId(currentMatch, winnerId);
        UUID previousLoserId = resolveLoserId(currentMatch, currentMatch.getWinnerId());

        Match updatedCurrentMatch = currentMatch.toBuilder()
                .winner(createInscriptionReference(winnerId))
                .result(scoreString.trim())
                .build();

        Match savedCurrentMatch = matchRepository.save(updatedCurrentMatch);

        if (currentMatch.getNextMatch() != null && currentMatch.getNextMatch().getId() != null) {
            Match nextMatch = matchRepository.findById(currentMatch.getNextMatch().getId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Next match", currentMatch.getNextMatch().getId()));

            Match updatedNextMatch = placeWinnerInNextMatch(nextMatch, winnerId, currentMatch.getWinnerId());
            matchRepository.save(updatedNextMatch);
        }

        if (currentMatch.getLoserNextMatch() != null && currentMatch.getLoserNextMatch().getId() != null && loserId != null) {
            Match loserNextMatch = matchRepository.findById(currentMatch.getLoserNextMatch().getId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Next match", currentMatch.getLoserNextMatch().getId()));

            Match updatedLoserNextMatch = placeLoserInNextMatch(loserNextMatch, loserId, previousLoserId);
            matchRepository.save(updatedLoserNextMatch);
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

    private Match placeWinnerInNextMatch(Match nextMatch, UUID winnerId, UUID previousWinnerId) {
        if (previousWinnerId != null) {
            if (previousWinnerId.equals(nextMatch.getFirstInscriptionId())) {
                return nextMatch.toBuilder()
                        .firstInscription(createInscriptionReference(winnerId))
                        .secondInscription(winnerId.equals(nextMatch.getSecondInscriptionId()) ? null : nextMatch.getSecondInscription())
                        .build();
            }

            if (previousWinnerId.equals(nextMatch.getSecondInscriptionId())) {
                return nextMatch.toBuilder()
                        .firstInscription(winnerId.equals(nextMatch.getFirstInscriptionId()) ? null : nextMatch.getFirstInscription())
                        .secondInscription(createInscriptionReference(winnerId))
                        .build();
            }
        }

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

    private Match placeLoserInNextMatch(Match nextMatch, UUID loserId, UUID previousLoserId) {
        if (previousLoserId != null) {
            if (previousLoserId.equals(nextMatch.getFirstInscriptionId())) {
                return nextMatch.toBuilder()
                        .firstInscription(createInscriptionReference(loserId))
                        .secondInscription(loserId.equals(nextMatch.getSecondInscriptionId()) ? null : nextMatch.getSecondInscription())
                        .build();
            }

            if (previousLoserId.equals(nextMatch.getSecondInscriptionId())) {
                return nextMatch.toBuilder()
                        .firstInscription(loserId.equals(nextMatch.getFirstInscriptionId()) ? null : nextMatch.getFirstInscription())
                        .secondInscription(createInscriptionReference(loserId))
                        .build();
            }
        }

        if (loserId.equals(nextMatch.getFirstInscriptionId()) || loserId.equals(nextMatch.getSecondInscriptionId())) {
            return nextMatch;
        }

        if (nextMatch.getFirstInscriptionId() == null) {
            return nextMatch.toBuilder()
                    .firstInscription(createInscriptionReference(loserId))
                    .build();
        }

        if (nextMatch.getSecondInscriptionId() == null) {
            return nextMatch.toBuilder()
                    .secondInscription(createInscriptionReference(loserId))
                    .build();
        }

        throw new InvalidArgumentException("El partido de consolación ya tiene los dos participantes asignados.");
    }

    private UUID resolveLoserId(Match match, UUID winnerId) {
        if (match == null || winnerId == null) {
            return null;
        }

        UUID firstInscriptionId = match.getFirstInscriptionId();
        UUID secondInscriptionId = match.getSecondInscriptionId();

        if (winnerId.equals(firstInscriptionId)) {
            return secondInscriptionId;
        }

        if (winnerId.equals(secondInscriptionId)) {
            return firstInscriptionId;
        }

        return null;
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
