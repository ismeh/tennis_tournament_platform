package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.events.TournamentUpdateEvent;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentUpdatePublisher;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final CourtRepository courtRepository;
    private final ScheduleConfigRepository scheduleConfigRepository;
    private final TournamentUpdatePublisher tournamentUpdatePublisher;
    private final TransactionTemplate transactionTemplate;
    private final TournamentService tournamentService;
    private static final int MAX_CONCURRENT_MODIFICATION_ATTEMPTS = 3;
    private static final Duration DEFAULT_MATCH_INTERVAL = Duration.ofHours(1);

    public Match update(Match match) {
        return matchRepository.save(match);
    }

    public Match update(Match match, String requesterEmail) {
        UUID tournamentId = match.getTournamentId();
        if (tournamentId == null) {
            throw new InvalidArgumentException("El torneo es obligatorio.");
        }

        tournamentService.assertTournamentAdmin(tournamentId, requesterEmail);
        return matchRepository.save(match);
    }

    public Match recordResult(UUID tournamentId, UUID matchId, UUID winnerId, String scoreString, MatchStatus status, String requesterEmail) {
        Match updatedMatch = retryOnConcurrentModification(() -> transactionTemplate.execute(txStatus ->
                doRecordResult(tournamentId, matchId, winnerId, scoreString, status, requesterEmail)
        ));
        tournamentUpdatePublisher.publish(TournamentUpdateEvent.matchResultUpdated(tournamentId, matchId));
        return updatedMatch;
    }

    private Match doRecordResult(UUID tournamentId, UUID matchId, UUID winnerId, String scoreString, MatchStatus status, String requesterEmail) {
        if (tournamentId == null) {
            throw new InvalidArgumentException("El torneo es obligatorio.");
        }
        if (matchId == null) {
            throw new InvalidArgumentException("El partido es obligatorio.");
        }

        boolean isWalkover = status == MatchStatus.WALKOVER;
        boolean isRetired = status == MatchStatus.RETIRED;
        boolean hasWinner = winnerId != null;
        boolean hasScore = scoreString != null && !scoreString.isBlank();

        if (!hasWinner && !hasScore && !isWalkover && !isRetired) {
            throw new InvalidArgumentException("Debes proporcionar al menos el resultado, el ganador o el tipo de estado especial.");
        }

        var tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        if (tournament.getState() != TournamentStatus.IN_PROGRESS) {
            throw new InvalidArgumentException("Solo se pueden registrar resultados en torneos en curso. Estado actual: " + tournament.getState());
        }

        tournamentService.assertTournamentAdmin(tournamentId, requesterEmail);

        Match currentMatch = matchRepository.findByIdAndTournamentId(matchId, tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));

        MatchStatus currentStatus = currentMatch.getStatus();
        if (currentStatus != null && currentStatus != MatchStatus.PENDING && currentStatus != MatchStatus.IN_PROGRESS) {
            throw new InvalidArgumentException("No se puede modificar el resultado de un partido que ya está en estado " + currentStatus + ".");
        }

        if (hasWinner && !winnerId.equals(currentMatch.getFirstInscriptionId()) && !winnerId.equals(currentMatch.getSecondInscriptionId())) {
            throw new InvalidArgumentException("El ganador debe ser uno de los participantes del partido.");
        }

        MatchStatus resolvedStatus = resolveStatus(status, hasWinner, hasScore, isWalkover, isRetired);

        var builder = currentMatch.toBuilder();
        builder.status(resolvedStatus);

        if (isWalkover || isRetired) {
            if (hasWinner) {
                builder.winner(createInscriptionReference(winnerId));
            } else {
                UUID autoWinner = autoResolveWinner(currentMatch, isWalkover);
                if (autoWinner != null) {
                    builder.winner(createInscriptionReference(autoWinner));
                    winnerId = autoWinner;
                }
            }
            builder.result(isWalkover ? "Walkover" : "Retirada");
        } else {
            if (hasScore) {
                builder.result(scoreString.trim());
            }
            if (hasWinner) {
                builder.winner(createInscriptionReference(winnerId));
            }
        }

        Match savedCurrentMatch = matchRepository.save(builder.build());

        if (winnerId != null) {
            UUID loserId = resolveLoserId(currentMatch, winnerId);
            UUID previousLoserId = resolveLoserId(currentMatch, currentMatch.getWinnerId());

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
        }

        return savedCurrentMatch;
    }

    private MatchStatus resolveStatus(MatchStatus requested, boolean hasWinner, boolean hasScore, boolean isWalkover, boolean isRetired) {
        if (requested != null && requested != MatchStatus.PENDING && requested != MatchStatus.IN_PROGRESS) {
            return requested;
        }
        if (hasWinner || hasScore) {
            return MatchStatus.COMPLETED;
        }
        return MatchStatus.IN_PROGRESS;
    }

    private UUID autoResolveWinner(Match match, boolean isWalkover) {
        boolean firstAbsent = match.getFirstInscriptionId() == null;
        boolean secondAbsent = match.getSecondInscriptionId() == null;

        if (isWalkover) {
            if (firstAbsent && !secondAbsent) {
                return match.getSecondInscriptionId();
            }
            if (secondAbsent && !firstAbsent) {
                return match.getFirstInscriptionId();
            }
        }
        return null;
    }

    public List<Match> findByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentId(tournamentId);
    }

    public Optional<Match> findById(String id) {
        return matchRepository.findById(id);
    }

    public Match schedule(UUID tournamentId, UUID matchId, UUID courtId, LocalDateTime scheduledAt, ScheduleTimeType scheduleTimeType, boolean cascade, String requesterEmail) {
        Match updatedMatch = retryOnConcurrentModification(() -> transactionTemplate.execute(status ->
                doSchedule(tournamentId, matchId, courtId, scheduledAt, scheduleTimeType, cascade, requesterEmail)
        ));
        tournamentUpdatePublisher.publish(TournamentUpdateEvent.matchScheduleUpdated(tournamentId, matchId));
        return updatedMatch;
    }

    private Match doSchedule(UUID tournamentId, UUID matchId, UUID courtId, LocalDateTime scheduledAt, ScheduleTimeType scheduleTimeType, boolean cascade, String requesterEmail) {
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
        tournamentService.assertTournamentAdmin(tournamentId, requesterEmail);

        ScheduleTimeType resolvedType = scheduleTimeType != null ? scheduleTimeType : ScheduleTimeType.EXACT;
        var tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        if (tournament.getState() != TournamentStatus.IN_PROGRESS) {
            throw new InvalidArgumentException("Solo se pueden programar partidos en torneos en curso. Estado actual: " + tournament.getState());
        }

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

        ScheduleConfig scheduleConfig = scheduleConfigRepository.findByTournamentId(tournamentId).orElse(null);
        Duration matchInterval = scheduleConfig != null && scheduleConfig.getMatchDurationMinutes() > 0
                ? Duration.ofMinutes(scheduleConfig.getMatchDurationMinutes())
                : DEFAULT_MATCH_INTERVAL;

        List<Match> matchesToCascade = cascade ? resolveCascadeMatches(tournamentMatches, currentMatch) : List.of(currentMatch);
        Set<UUID> cascadeMatchIds = matchesToCascade.stream()
                .map(Match::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (!cascade) {
            boolean courtBusy = tournamentMatches.stream()
                    .filter(match -> !cascadeMatchIds.contains(match.getId()))
                    .anyMatch(match -> isSameCourtSlot(match, courtId, scheduledAt, matchInterval));

            if (courtBusy) {
                throw new InvalidArgumentException("La pista ya está ocupada en esa hora.");
            }

            boolean playerConflict = tournamentMatches.stream()
                    .filter(match -> !cascadeMatchIds.contains(match.getId()))
                    .filter(match -> match.getScheduledAt() != null && match.getScheduledAt().equals(scheduledAt))
                    .anyMatch(match -> sharesPlayer(match, currentMatch));

            if (playerConflict) {
                throw new InvalidArgumentException("Un jugador ya tiene otro partido programado en esa hora.");
            }

            Match updatedMatch = currentMatch.toBuilder()
                    .scheduledAt(scheduledAt)
                    .scheduleTimeType(resolvedType)
                    .courtId(court.getId())
                    .court(court.getName())
                    .build();

            return matchRepository.save(updatedMatch);
        }

        List<Court> activeCourts = courtRepository.findByTournamentId(tournamentId).stream()
                .filter(Court::isActive)
                .toList();

        List<TimeSlot> timeSlots = scheduleConfig != null ? scheduleConfig.getTimeSlots() : List.of();

        Duration shift = currentMatch.getScheduledAt() != null
                ? Duration.between(currentMatch.getScheduledAt(), scheduledAt)
                : null;

        List<Match> remaining = matchesToCascade.stream()
                .filter(m -> !m.getId().equals(currentMatch.getId()))
                .toList();

        List<Match> nonCascadeMatches = tournamentMatches.stream()
                .filter(m -> !cascadeMatchIds.contains(m.getId()))
                .toList();

        Match updatedCurrent = currentMatch.toBuilder()
                .scheduledAt(scheduledAt)
                .scheduleTimeType(resolvedType)
                .courtId(court.getId())
                .court(court.getName())
                .build();

        List<Match> updatedOtherMatches = new java.util.ArrayList<>();
        int unscheduledIndex = 0;
        for (Match m : remaining) {
            updatedOtherMatches.add(updateCascadeMatch(m, currentMatch, court, scheduledAt, resolvedType, shift, unscheduledIndex, matchInterval, timeSlots, activeCourts.size()));
            if (m.getScheduledAt() == null && !m.getId().equals(currentMatch.getId())) {
                unscheduledIndex++;
            }
        }

        List<Match> allCascade = new java.util.ArrayList<>();
        allCascade.add(updatedCurrent);
        allCascade.addAll(updatedOtherMatches);

        assignCascadeCourts(allCascade, tournamentMatches, cascadeMatchIds, activeCourts, matchInterval);

        java.util.List<Match> resolvedCascade = new java.util.ArrayList<>();
        for (int i = 0; i < allCascade.size(); i++) {
            Match m = allCascade.get(i);
            if (m.getScheduledAt() != null && m.getCourtId() != null) {
                LocalDateTime availableSlot = findNextAvailableSlot(m.getScheduledAt(), m.getCourtId(),
                        m, nonCascadeMatches, resolvedCascade, timeSlots, matchInterval);
                allCascade.set(i, m.toBuilder().scheduledAt(availableSlot).build());
            }
            resolvedCascade.add(allCascade.get(i));
        }

        updatedCurrent = allCascade.get(0);
        updatedOtherMatches = new java.util.ArrayList<>(allCascade.subList(1, allCascade.size()));

        List<Match> updatedMatches = new java.util.ArrayList<>();
        updatedMatches.add(updatedCurrent);
        updatedMatches.addAll(updatedOtherMatches);

        validateCascadeDates(updatedMatches, tournament.getPlayPeriod().startDate(), tournament.getPlayPeriod().endDate());

        return matchRepository.saveAll(updatedMatches).stream()
                .filter(match -> matchId.equals(match.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
    }

    private List<Match> resolveCascadeMatches(List<Match> tournamentMatches, Match currentMatch) {
        if (currentMatch.getScheduledAt() != null) {
            return tournamentMatches.stream()
                    .filter(match -> match.getScheduledAt() != null)
                    .filter(match -> !match.getScheduledAt().isBefore(currentMatch.getScheduledAt()))
                    .sorted(Comparator
                            .comparing(Match::getScheduledAt)
                            .thenComparing(match -> match.getRoundNumber() != null ? match.getRoundNumber() : Integer.MAX_VALUE)
                            .thenComparing(match -> match.getBracketPosition() != null ? match.getBracketPosition() : Integer.MAX_VALUE)
                            .thenComparing(Match::getId))
                    .toList();
        }

        Comparator<Match> bracketOrder = Comparator
                .<Match, Integer>comparing(match -> match.getRoundNumber() != null ? match.getRoundNumber() : Integer.MAX_VALUE)
                .thenComparing(match -> match.getBracketPosition() != null ? match.getBracketPosition() : Integer.MAX_VALUE)
                .thenComparing(Match::getId);

        List<Match> unscheduled = tournamentMatches.stream()
                .filter(match -> match.getScheduledAt() == null)
                .sorted(bracketOrder)
                .toList();

        List<Match> scheduledAfter = tournamentMatches.stream()
                .filter(match -> match.getScheduledAt() != null)
                .sorted(Comparator
                        .comparing(Match::getScheduledAt)
                        .thenComparing(bracketOrder))
                .toList();

        List<Match> result = new java.util.ArrayList<>(unscheduled);
        result.addAll(scheduledAfter);
        return result;
    }

    private Match updateCascadeMatch(Match match, Match currentMatch, Court court, LocalDateTime scheduledAt, ScheduleTimeType scheduleTimeType, Duration shift, int unscheduledIndex, Duration matchInterval, List<TimeSlot> timeSlots, int courtCount) {
        if (match.getId().equals(currentMatch.getId())) {
            return match.toBuilder()
                    .scheduledAt(scheduledAt)
                    .scheduleTimeType(scheduleTimeType)
                    .courtId(court.getId())
                    .court(court.getName())
                    .build();
        }

        if (match.getScheduledAt() != null && shift != null) {
            LocalDateTime shifted = match.getScheduledAt().plus(shift);
            return match.toBuilder()
                    .scheduledAt(snapToTimeSlots(shifted, timeSlots))
                    .build();
        }

        int batchIndex = courtCount > 0 ? unscheduledIndex / courtCount : unscheduledIndex;
        LocalDateTime baseTime = scheduledAt.plus(matchInterval.multipliedBy(batchIndex + 1));
        LocalDateTime snapped = snapToTimeSlots(baseTime, timeSlots);
        return match.toBuilder()
                .scheduledAt(snapped)
                .scheduleTimeType(scheduleTimeType)
                .build();
    }

    private LocalDateTime findNextAvailableSlot(LocalDateTime from, UUID courtId, Match candidate,
                                                List<Match> nonCascadeMatches, List<Match> scheduledCascadeMatches,
                                                List<TimeSlot> timeSlots, Duration matchInterval) {
        LocalDateTime cursor = snapToTimeSlots(from, timeSlots);
        int maxAttempts = 200;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            final LocalDateTime slot = cursor;
            boolean courtFree = courtId == null || nonCascadeMatches.stream()
                    .noneMatch(m -> isSameCourtSlot(m, courtId, slot, matchInterval));
            if (courtFree && courtId != null) {
                courtFree = scheduledCascadeMatches.stream()
                        .noneMatch(m -> isSameCourtSlot(m, courtId, slot, matchInterval));
            }

            boolean playersFree = true;
            if (courtFree) {
                playersFree = nonCascadeMatches.stream()
                        .filter(m -> m.getScheduledAt() != null && m.getScheduledAt().equals(slot))
                        .noneMatch(m -> sharesPlayer(m, candidate));
                if (playersFree) {
                    playersFree = scheduledCascadeMatches.stream()
                            .filter(m -> m.getScheduledAt() != null && m.getScheduledAt().equals(slot))
                            .noneMatch(m -> sharesPlayer(m, candidate));
                }
            }

            if (courtFree && playersFree) {
                return cursor;
            }

            cursor = snapToTimeSlots(cursor.plus(matchInterval), timeSlots);
        }

        return cursor;
    }

    private LocalDateTime snapToTimeSlots(LocalDateTime dateTime, List<TimeSlot> timeSlots) {
        if (timeSlots.isEmpty()) {
            return dateTime;
        }

        LocalTime time = dateTime.toLocalTime();
        for (TimeSlot slot : timeSlots) {
            if (!time.isBefore(slot.startTime()) && time.isBefore(slot.endTime())) {
                return dateTime;
            }
        }

        for (TimeSlot slot : timeSlots) {
            if (slot.startTime().isAfter(time)) {
                return LocalDateTime.of(dateTime.toLocalDate(), slot.startTime());
            }
        }

        return dateTime.plusDays(1).with(timeSlots.get(0).startTime());
    }

    private void assignCascadeCourts(List<Match> cascadeMatches, List<Match> tournamentMatches, Set<UUID> cascadeMatchIds, List<Court> activeCourts, Duration matchInterval) {
        if (activeCourts.isEmpty()) {
            return;
        }

        List<Match> nonCascadeMatches = tournamentMatches.stream()
                .filter(m -> !cascadeMatchIds.contains(m.getId()))
                .toList();

        for (int i = 0; i < cascadeMatches.size(); i++) {
            Match candidate = cascadeMatches.get(i);
            if (candidate.getScheduledAt() == null) {
                continue;
            }

            for (Court c : activeCourts) {
                boolean occupied = nonCascadeMatches.stream()
                        .anyMatch(m -> isSameCourtSlot(m, c.getId(), candidate.getScheduledAt(), matchInterval));

                if (!occupied) {
                    occupied = cascadeMatches.stream()
                            .filter(other -> !other.getId().equals(candidate.getId()))
                            .anyMatch(other -> isSameCourtSlot(other, c.getId(), candidate.getScheduledAt(), matchInterval));
                }

                if (!occupied) {
                    cascadeMatches.set(i, candidate.toBuilder()
                            .courtId(c.getId())
                            .court(c.getName())
                            .build());
                    break;
                }
            }
        }
    }

    private void validateCascadeCourtAvailability(List<Match> tournamentMatches, List<Match> updatedMatches, Set<UUID> cascadeMatchIds, Duration matchInterval) {
        List<Match> nonCascadeMatches = tournamentMatches.stream()
                .filter(match -> !cascadeMatchIds.contains(match.getId()))
                .toList();

        for (Match updatedMatch : updatedMatches) {
            boolean courtBusy = nonCascadeMatches.stream()
                    .anyMatch(match -> isSameCourtSlot(match, updatedMatch.getCourtId(), updatedMatch.getScheduledAt(), matchInterval));

            if (!courtBusy) {
                courtBusy = updatedMatches.stream()
                        .filter(other -> !other.getId().equals(updatedMatch.getId()))
                        .anyMatch(other -> isSameCourtSlot(other, updatedMatch.getCourtId(), updatedMatch.getScheduledAt(), matchInterval));
            }

            if (courtBusy) {
                throw new InvalidArgumentException("La replanificación en cascada ocupa una pista que ya tiene otro partido asignado.");
            }
        }
    }

    private void validateCascadePlayerAvailability(List<Match> tournamentMatches, List<Match> updatedMatches, Set<UUID> cascadeMatchIds) {
        List<Match> nonCascadeMatches = tournamentMatches.stream()
                .filter(match -> !cascadeMatchIds.contains(match.getId()))
                .toList();

        for (Match updatedMatch : updatedMatches) {
            boolean playerConflict = nonCascadeMatches.stream()
                    .filter(match -> match.getScheduledAt() != null && match.getScheduledAt().equals(updatedMatch.getScheduledAt()))
                    .anyMatch(match -> sharesPlayer(match, updatedMatch));

            if (!playerConflict) {
                playerConflict = updatedMatches.stream()
                        .filter(other -> !other.getId().equals(updatedMatch.getId()))
                        .filter(other -> other.getScheduledAt() != null && other.getScheduledAt().equals(updatedMatch.getScheduledAt()))
                        .anyMatch(other -> sharesPlayer(other, updatedMatch));
            }

            if (playerConflict) {
                throw new InvalidArgumentException("La replanificación en cascada causa un conflicto de horario para un jugador.");
            }
        }
    }

    private void validateCascadeDates(List<Match> updatedMatches, java.time.LocalDate playStartDate, java.time.LocalDate playEndDate) {
        boolean outsidePlayPeriod = updatedMatches.stream()
                .anyMatch(match -> match.getScheduledAt().toLocalDate().isBefore(playStartDate)
                        || match.getScheduledAt().toLocalDate().isAfter(playEndDate));

        if (outsidePlayPeriod) {
            throw new InvalidArgumentException("La replanificación en cascada debe quedar dentro del periodo de juego del torneo.");
        }
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

    private boolean isSameCourtSlot(Match match, UUID courtId, LocalDateTime scheduledAt, Duration matchInterval) {
        if (courtId == null || scheduledAt == null || match.getCourtId() == null || match.getScheduledAt() == null) {
            return false;
        }
        if (!courtId.equals(match.getCourtId())) {
            return false;
        }
        LocalDateTime start1 = scheduledAt;
        LocalDateTime end1 = scheduledAt.plus(matchInterval);
        LocalDateTime start2 = match.getScheduledAt();
        LocalDateTime end2 = match.getScheduledAt().plus(matchInterval);

        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean sharesPlayer(Match match1, Match match2) {
        if (match1.getFirstInscriptionId() == null && match1.getSecondInscriptionId() == null) {
            return false;
        }
        if (match2.getFirstInscriptionId() == null && match2.getSecondInscriptionId() == null) {
            return false;
        }

        boolean sameFirst = match1.getFirstInscriptionId() != null
                && match1.getFirstInscriptionId().equals(match2.getFirstInscriptionId());
        boolean sameSecond = match1.getSecondInscriptionId() != null
                && match1.getSecondInscriptionId().equals(match2.getSecondInscriptionId());
        boolean crossFirst = match1.getFirstInscriptionId() != null
                && match1.getFirstInscriptionId().equals(match2.getSecondInscriptionId());
        boolean crossSecond = match1.getSecondInscriptionId() != null
                && match1.getSecondInscriptionId().equals(match2.getFirstInscriptionId());

        return sameFirst || sameSecond || crossFirst || crossSecond;
    }
}
