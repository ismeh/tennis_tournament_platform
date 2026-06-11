package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;
    private static final int MAX_CONCURRENT_MODIFICATION_ATTEMPTS = 3;

    public Match update(Match match) {
        return matchRepository.save(match);
    }

    public Match recordResult(UUID tournamentId, UUID matchId, UUID winnerId, String scoreString) {
        return retryOnConcurrentModification(() -> transactionTemplate.execute(status ->
                doRecordResult(tournamentId, matchId, winnerId, scoreString)
        ));
    }

    private Match doRecordResult(UUID tournamentId, UUID matchId, UUID winnerId, String scoreString) {
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

        Match currentMatch = matchRepository.findByIdAndTournamentId(matchId, tournamentId)
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

            Match updatedNextMatch = placeWinnerInNextMatch(nextMatch, currentMatch, winnerId);
            matchRepository.save(updatedNextMatch);
        }

        if (currentMatch.getLoserNextMatch() != null && currentMatch.getLoserNextMatch().getId() != null && loserId != null) {
            Match loserNextMatch = matchRepository.findById(currentMatch.getLoserNextMatch().getId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Next match", currentMatch.getLoserNextMatch().getId()));

            Match updatedLoserNextMatch = placeLoserInNextMatch(loserNextMatch, currentMatch, loserId, previousLoserId);
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

    public Match schedule(UUID tournamentId, UUID matchId, UUID courtId, LocalDateTime scheduledAt, ScheduleTimeType scheduleTimeType) {
        return retryOnConcurrentModification(() -> transactionTemplate.execute(status ->
                doSchedule(tournamentId, matchId, courtId, scheduledAt, scheduleTimeType)
        ));
    }

    private Match doSchedule(UUID tournamentId, UUID matchId, UUID courtId, LocalDateTime scheduledAt, ScheduleTimeType scheduleTimeType) {
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

    private Match retryOnConcurrentModification(java.util.function.Supplier<Match> operation) {
        ObjectOptimisticLockingFailureException lastException = null;

        for (int attempt = 1; attempt <= MAX_CONCURRENT_MODIFICATION_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (ObjectOptimisticLockingFailureException ex) {
                lastException = ex;
                if (attempt == MAX_CONCURRENT_MODIFICATION_ATTEMPTS) {
                    throw ex;
                }
                sleepBeforeRetry(attempt);
            }
        }

        throw lastException;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private Match placeWinnerInNextMatch(Match nextMatch, Match sourceMatch, UUID winnerId) {
        UUID previousWinnerId = sourceMatch.getWinnerId();
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

        Boolean firstSlot = shouldUseFirstSlot(sourceMatch);
        if (firstSlot != null) {
            return placeInscriptionInNextMatchSlot(nextMatch, winnerId, firstSlot, "El siguiente partido ya tiene ocupado el hueco de este ganador.");
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

    private Match placeLoserInNextMatch(Match nextMatch, Match sourceMatch, UUID loserId, UUID previousLoserId) {
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

        Boolean firstSlot = shouldUseFirstSlot(sourceMatch);
        if (firstSlot != null) {
            return placeInscriptionInNextMatchSlot(nextMatch, loserId, firstSlot, "El partido de consolación ya tiene ocupado el hueco de este perdedor.");
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

    private Match placeInscriptionInNextMatchSlot(Match nextMatch, UUID inscriptionId, boolean firstSlot, String occupiedMessage) {
        if (firstSlot) {
            if (nextMatch.getFirstInscriptionId() != null && !inscriptionId.equals(nextMatch.getFirstInscriptionId())) {
                throw new InvalidArgumentException(occupiedMessage);
            }
            return nextMatch.toBuilder()
                    .firstInscription(createInscriptionReference(inscriptionId))
                    .build();
        }

        if (nextMatch.getSecondInscriptionId() != null && !inscriptionId.equals(nextMatch.getSecondInscriptionId())) {
            throw new InvalidArgumentException(occupiedMessage);
        }
        return nextMatch.toBuilder()
                .secondInscription(createInscriptionReference(inscriptionId))
                .build();
    }

    private Boolean shouldUseFirstSlot(Match sourceMatch) {
        if (sourceMatch.getBracketPosition() == null) {
            return null;
        }

        return sourceMatch.getBracketPosition() % 2 == 0;
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
