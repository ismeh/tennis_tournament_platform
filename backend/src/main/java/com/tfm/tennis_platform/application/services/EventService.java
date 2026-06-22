package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.domain.models.validation.StageSequenceValidator;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import static com.tfm.tennis_platform.domain.models.Event.buildEventName;
import static com.tfm.tennis_platform.domain.models.Event.createOrUpdateEvent;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final TournamentRepository tournamentRepository;
    private final InscriptionRepository inscriptionRepository;
    private final MatchRepository matchRepository;
    private final CourtRepository courtRepository;
    private final TournamentService tournamentService;
    private final StageGenerationService stageGenerationService;
    private final DrawGenerationService drawGenerationService;
    private final MatchGenerationService matchGenerationService;
        private final MatchPersistenceService matchPersistenceService;

    @Transactional
    public Tournament replaceAllEvents(UUID tournamentId, EventCommand eventCommand, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        for (EventCommand.EventItem item : eventCommand.events()) {
            if (item.stages() != null && !item.stages().isEmpty()) {
                StageSequenceValidator.validate(item.stages());
            }
        }

        List<Event> events = eventCommand.events().stream()
                .map(item -> {
                    var stages = stageGenerationService.generateStagesForEvent(
                            buildEventName(item),

                            item.categoryId(),
                            item.gender(),
                            item.stages()
                    );
                    return createOrUpdateEvent(tournamentId, item, stages);
                })
                .toList();

        return tournamentRepository.save(tournament.setEvents(events));
    }

    @Transactional
    public Tournament removeEventFromTournament(UUID tournamentId, UUID eventId, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        List<Event> updatedEvents = tournament.getEvents().stream()
                .filter(event -> !eventId.equals(event.getId()))
                .toList();

        if (updatedEvents.size() == tournament.getEvents().size()) {
            throw new ResourceNotFoundException("No se encontró el evento dentro del torneo.");
        }

        Tournament tournamentWithoutEvent = tournament.toBuilder()
                .events(updatedEvents)
                .build();

        return tournamentRepository.save(tournamentWithoutEvent);
    }

    @Transactional
    public Tournament generateDrawsForEvent(UUID tournamentId, UUID eventId, String requesterEmail) {
        log.info("Generating draws for event {} in tournament {}", eventId, tournamentId);
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        Event event = tournament.getEvents().stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento dentro del torneo."));

        List<Inscription> eventInscriptions = inscriptionRepository.findByEventId(eventId);

        if (eventInscriptions.isEmpty()) {
            throw new InvalidArgumentException("Este evento todavía no tiene jugadores inscritos.");
        }

        List<Stage> updatedStages = event.getStages().stream()
                .map(stage -> {
                    List<Draw> draws = drawGenerationService
                            .generateDrawsForStage(stage, eventInscriptions);

                    // At this point we only attach empty draws; matches will be generated after
                    List<Draw> drawsWithoutMatches = draws.stream()
                            .map(draw -> draw.toBuilder().matches(List.of()).build())
                            .collect(Collectors.toList());

                    return stage.toBuilder()
                            .clearDraws()
                            .draws(drawsWithoutMatches)
                            .build();
                })
                .collect(Collectors.toList());

        Event updatedEvent = event.toBuilder()
                .clearStages()
                .stages(updatedStages)
                .build();

        List<Event> updatedEvents = tournament.getEvents().stream()
                .map(e -> e.getId().equals(eventId) ? updatedEvent : e)
                .collect(Collectors.toList());

                Tournament updatedTournament = tournament.toBuilder()
                                .events(updatedEvents)
                                .build();

                // Persist tournament + draws first so draws exist in DB (draw ids are used by matches)
                log.info("About to save updatedTournament events count={}", updatedTournament.getEvents() == null ? 0 : updatedTournament.getEvents().size());
                Tournament persistedTournament = tournamentRepository.save(updatedTournament);

                                // Diagnostic logging: inspect persistedTournament for duplicate stage/draw ids
                                log.info("Persisted tournament events count={}", persistedTournament.getEvents() == null ? 0 : persistedTournament.getEvents().size());
                                for (var ev : persistedTournament.getEvents()) {
                                        log.info("Event id={} stagesCount={}", ev.getId(), ev.getStages() == null ? 0 : ev.getStages().size());
                                        if (ev.getStages() != null) {
                                                var seenStageIds = new java.util.HashMap<java.util.UUID, Integer>();
                                                for (var st : ev.getStages()) {
                                                        seenStageIds.put(st.getId(), seenStageIds.getOrDefault(st.getId(), 0) + 1);
                                                        log.info(" Event {} stage id={} drawsCount={}", ev.getId(), st.getId(), st.getDraws() == null ? 0 : st.getDraws().size());
                                                }
                                                seenStageIds.forEach((id, count) -> {
                                                        if (count > 1) log.warn("Duplicate stage id {} appears {} times in event {}", id, count, ev.getId());
                                                });
                                                // log stage id list
                                                log.info(" Persisted event {} stageIds={}", ev.getId(), ev.getStages().stream().map(s -> s.getId().toString()).collect(Collectors.joining(",")));
                                        }
                                }

                // Now generate and persist matches for each draw. Use retries on optimistic lock failures.
                Map<UUID, List<Match>> matchesByDraw = new java.util.HashMap<>();
                List<Court> courts = courtRepository.findByTournamentId(tournamentId);
                LocalDateTime firstMatchStart = tournament.getStartTime() != null
                        ? LocalDateTime.of(tournament.getPlayPeriod().startDate(), tournament.getStartTime())
                        : null;
                java.util.concurrent.atomic.AtomicInteger scheduleIndex = new java.util.concurrent.atomic.AtomicInteger(0);
                for (Event ev : persistedTournament.getEvents()) {
                        if (!ev.getId().equals(eventId)) continue;

                        log.debug("Processing event {} with stages count {}", ev.getId(), ev.getStages() == null ? 0 : ev.getStages().size());

                        for (Stage st : ev.getStages()) {
                                log.debug(" Processing stage id={} current stagesCount={}", st.getId(), ev.getStages() == null ? 0 : ev.getStages().size());
                                for (Draw dr : st.getDraws()) {
                                        log.debug("  Processing draw id={} current stagesCount={}", dr.getId(), ev.getStages() == null ? 0 : ev.getStages().size());
                                        List<Match> matches = scheduleGeneratedMatches(
                                                        matchGenerationService.generateMatchesForDraw(dr, eventInscriptions),
                                                        courts,
                                                        firstMatchStart,
                                                        scheduleIndex
                                        );

                                        matchesByDraw.put(dr.getId(), matches);
                                }
                        }

                        linkConsolationLoserDestinations(ev, matchesByDraw);
                        List<Match> savedMatches = matchPersistenceService.saveMatches(sortMatchesForPersistence(ev, matchesByDraw));
                        replaceWithSavedMatches(matchesByDraw, savedMatches);
                }

                // Rebuild returned Tournament domain object with matches attached to draws
                                List<Event> rebuiltEvents = persistedTournament.getEvents().stream()
                                                                .map(ev -> {
                                                                                if (!ev.getId().equals(eventId)) return ev;

                                                                                // Build stages with rebuilt draws and dedupe by stage id, preferring richer stage
                                                                                java.util.Map<UUID, Stage> stagesById = new java.util.LinkedHashMap<>();

                                                                                for (Stage st : ev.getStages()) {
                                                                                        List<Draw> rebuiltDraws = st.getDraws().stream()
                                                                                                                        .map(dr -> dr.toBuilder()
                                                                                                                                                        .matches(matchesByDraw.getOrDefault(dr.getId(), java.util.List.of()))
                                                                                                                                                        .build())
                                                                                                                        .collect(Collectors.toList());
                                                                                        Stage candidate = st.toBuilder().clearDraws().draws(rebuiltDraws).build();

                                                                                        Stage existing = stagesById.get(candidate.getId());
                                                                                        if (existing == null) {
                                                                                                stagesById.put(candidate.getId(), candidate);
                                                                                                continue;
                                                                                        }

                                                                                        int existingScore = (existing.getDraws() == null) ? 0 : existing.getDraws().stream().mapToInt(d -> d.getMatches() == null ? 0 : d.getMatches().size()).sum();
                                                                                        int candidateScore = (candidate.getDraws() == null) ? 0 : candidate.getDraws().stream().mapToInt(d -> d.getMatches() == null ? 0 : d.getMatches().size()).sum();
                                                                                        if (candidateScore > existingScore) {
                                                                                                stagesById.put(candidate.getId(), candidate);
                                                                                        }
                                                                                }

                                                                                List<Stage> rebuiltStages = new java.util.ArrayList<>(stagesById.values());
                                                                                return ev.toBuilder().clearStages().stages(rebuiltStages).build();
                                                                })
                                                                .collect(Collectors.toList());

                                log.info("Rebuilt events count={}", rebuiltEvents == null ? 0 : rebuiltEvents.size());
                                for (var ev : rebuiltEvents) {
                                        log.info("Rebuilt event id={} stagesCount={}", ev.getId(), ev.getStages() == null ? 0 : ev.getStages().size());
                                        log.info(" Rebuilt event {} stageIds={}", ev.getId(), ev.getStages().stream().map(s -> s.getId().toString()).collect(Collectors.joining(",")));
                                }

                                Tournament returned = persistedTournament.toBuilder().events(rebuiltEvents).build();
                                log.info("Returning tournament events count={}", returned.getEvents() == null ? 0 : returned.getEvents().size());
                                return tournamentRepository.findById(tournamentId)
                                        .filter(reloadedTournament -> hasGeneratedEventMatches(reloadedTournament, eventId))
                                        .orElse(returned);
    }

    private void replaceWithSavedMatches(Map<UUID, List<Match>> matchesByDraw, List<Match> savedMatches) {
        Map<UUID, Match> savedMatchesById = mapMatchesById(savedMatches);
        if (savedMatchesById.isEmpty()) {
            return;
        }

        matchesByDraw.replaceAll((drawId, matches) -> matches.stream()
                .map(match -> savedMatchesById.getOrDefault(match.getId(), match))
                .toList());
    }

    private Map<UUID, Match> mapMatchesById(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return Map.of();
        }

        return matches.stream()
                .collect(Collectors.toMap(Match::getId, Function.identity()));
    }

    private boolean hasGeneratedEventMatches(Tournament tournament, UUID eventId) {
        if (tournament == null || tournament.getEvents() == null) {
            return false;
        }

        return tournament.getEvents().stream()
                .filter(event -> eventId.equals(event.getId()))
                .filter(event -> event.getStages() != null)
                .flatMap(event -> event.getStages().stream())
                .filter(stage -> stage.getDraws() != null)
                .flatMap(stage -> stage.getDraws().stream())
                .anyMatch(draw -> draw.getMatches() != null && !draw.getMatches().isEmpty());
    }

    private void linkConsolationLoserDestinations(Event event, Map<UUID, List<Match>> matchesByDraw) {
        if (event.getStages() == null || event.getStages().isEmpty()) {
            return;
        }

        linkMainStageLoserDestinations(event, matchesByDraw);
        linkDoubleEliminationLoserDestinations(event, matchesByDraw);
    }

    private void linkMainStageLoserDestinations(Event event, Map<UUID, List<Match>> matchesByDraw) {
        List<Draw> mainDraws = event.getStages().stream()
                .filter(stage -> StageType.MAIN.equals(stage.getStageType()))
                .flatMap(stage -> stage.getDraws().stream())
                .filter(draw -> DrawType.ELIMINATION.equals(draw.getDrawType()))
                .toList();

        List<Draw> consolationDraws = event.getStages().stream()
                .filter(stage -> StageType.CONSOLATION.equals(stage.getStageType()))
                .flatMap(stage -> stage.getDraws().stream())
                .filter(draw -> DrawType.CONSOLATION.equals(draw.getDrawType()))
                .toList();

        int pairs = Math.min(mainDraws.size(), consolationDraws.size());
        for (int index = 0; index < pairs; index++) {
            Draw mainDraw = mainDraws.get(index);
            Draw consolationDraw = consolationDraws.get(index);
            List<Match> sourceMatches = firstRoundContestedMatches(matchesByDraw.get(mainDraw.getId()));
            List<Match> targetMatches = firstRoundMatches(matchesByDraw.get(consolationDraw.getId()));

            if (sourceMatches.isEmpty() || targetMatches.isEmpty()) {
                continue;
            }

            List<Match> linkedSourceMatches = new java.util.ArrayList<>(matchesByDraw.get(mainDraw.getId()));
            int limit = Math.min(sourceMatches.size(), targetMatches.size() * 2);

            for (int sourceIndex = 0; sourceIndex < limit; sourceIndex++) {
                Match sourceMatch = sourceMatches.get(sourceIndex);
                Match targetMatch = targetMatches.get(sourceIndex / 2);
                int originalIndex = linkedSourceMatches.indexOf(sourceMatch);

                if (originalIndex >= 0) {
                    linkedSourceMatches.set(originalIndex, sourceMatch.toBuilder()
                            .loserNextMatch(targetMatch)
                            .build());
                }
            }

            matchesByDraw.put(mainDraw.getId(), linkedSourceMatches);
        }
    }

    private void linkDoubleEliminationLoserDestinations(Event event, Map<UUID, List<Match>> matchesByDraw) {
        List<Draw> eliminationDraws = event.getStages().stream()
                .filter(stage -> StageType.DOUBLE_ELIMINATION.equals(stage.getStageType()))
                .flatMap(stage -> stage.getDraws().stream())
                .filter(draw -> DrawType.ELIMINATION.equals(draw.getDrawType()))
                .toList();

        List<Draw> losersDraws = event.getStages().stream()
                .filter(stage -> StageType.DOUBLE_ELIMINATION.equals(stage.getStageType()))
                .flatMap(stage -> stage.getDraws().stream())
                .filter(draw -> DrawType.DOUBLE_ELIMINATION.equals(draw.getDrawType()))
                .toList();

        for (Draw eliminationDraw : eliminationDraws) {
            List<Match> winnersMatches = matchesByDraw.get(eliminationDraw.getId());
            if (winnersMatches == null || winnersMatches.isEmpty()) {
                continue;
            }

            Draw losersDraw = losersDraws.stream()
                    .findFirst()
                    .orElse(null);
            if (losersDraw == null) {
                continue;
            }

            List<Match> losersMatches = matchesByDraw.get(losersDraw.getId());
            if (losersMatches == null || losersMatches.isEmpty()) {
                continue;
            }

            linkWinnersLosersByRound(winnersMatches, losersMatches);

            Match grandFinal = createGrandFinal(eliminationDraw, winnersMatches, losersMatches);
            if (grandFinal != null) {
                linkWinnersFinalToGrandFinal(winnersMatches, grandFinal);
                linkLosersFinalToGrandFinal(losersMatches, grandFinal);
                winnersMatches.add(grandFinal);
            }
        }
    }

    private Match createGrandFinal(Draw eliminationDraw, List<Match> winnersMatches, List<Match> losersMatches) {
        int maxWinnersRound = winnersMatches.stream()
                .filter(m -> m.getRoundNumber() != null)
                .mapToInt(Match::getRoundNumber)
                .max()
                .orElse(0);

        int maxLosersRound = losersMatches.stream()
                .filter(m -> m.getRoundNumber() != null)
                .mapToInt(Match::getRoundNumber)
                .max()
                .orElse(0);

        if (maxWinnersRound == 0 || maxLosersRound == 0) {
            return null;
        }

        return Match.builder()
                .id(java.util.UUID.randomUUID())
                .drawId(eliminationDraw.getId())
                .roundNumber(maxWinnersRound + 1)
                .bracketPosition(0)
                .build();
    }

    private void linkWinnersFinalToGrandFinal(List<Match> winnersMatches, Match grandFinal) {
        int maxRound = winnersMatches.stream()
                .filter(m -> m.getRoundNumber() != null)
                .mapToInt(Match::getRoundNumber)
                .max()
                .orElse(0);

        List<Match> finalRoundMatches = winnersMatches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == maxRound
                        && m.getBracketPosition() != null && m.getBracketPosition() == 0)
                .toList();

        List<Match> updated = new java.util.ArrayList<>(winnersMatches);
        for (Match m : finalRoundMatches) {
            int idx = updated.indexOf(m);
            if (idx >= 0) {
                updated.set(idx, m.toBuilder().nextMatch(grandFinal).build());
            }
        }
        winnersMatches.clear();
        winnersMatches.addAll(updated);
    }

    private void linkLosersFinalToGrandFinal(List<Match> losersMatches, Match grandFinal) {
        int maxRound = losersMatches.stream()
                .filter(m -> m.getRoundNumber() != null)
                .mapToInt(Match::getRoundNumber)
                .max()
                .orElse(0);

        List<Match> finalRoundMatches = losersMatches.stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == maxRound
                        && m.getBracketPosition() != null && m.getBracketPosition() == 0)
                .toList();

        List<Match> updated = new java.util.ArrayList<>(losersMatches);
        for (Match m : finalRoundMatches) {
            int idx = updated.indexOf(m);
            if (idx >= 0) {
                updated.set(idx, m.toBuilder().nextMatch(grandFinal).build());
            }
        }
        losersMatches.clear();
        losersMatches.addAll(updated);
    }

    private void linkWinnersLosersByRound(List<Match> winnersMatches, List<Match> losersMatches) {
        List<Match> sortedWinners = winnersMatches.stream()
                .filter(m -> m.getRoundNumber() != null)
                .sorted(java.util.Comparator.comparing(Match::getRoundNumber))
                .toList();

        int maxWinnersRound = sortedWinners.stream()
                .mapToInt(m -> m.getRoundNumber())
                .max()
                .orElse(0);

        for (int wr = 1; wr < maxWinnersRound; wr++) {
            int finalWr = wr;
            List<Match> roundLosers = sortedWinners.stream()
                    .filter(m -> m.getRoundNumber() == finalWr
                            && m.getFirstInscriptionId() != null
                            && m.getSecondInscriptionId() != null)
                    .toList();

            if (roundLosers.isEmpty()) {
                continue;
            }

            int losersBracketRound = 2 * wr - 1;
            int finalLosersBracketRound = losersBracketRound;
            List<Match> targetRoundMatches = losersMatches.stream()
                    .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == finalLosersBracketRound)
                    .sorted(java.util.Comparator.comparing(Match::getBracketPosition))
                    .toList();

            if (targetRoundMatches.isEmpty()) {
                continue;
            }

            List<Match> updatedWinners = new java.util.ArrayList<>(winnersMatches);
            int limit = Math.min(roundLosers.size(), targetRoundMatches.size() * 2);

            for (int i = 0; i < limit; i++) {
                Match sourceMatch = roundLosers.get(i);
                Match targetMatch = targetRoundMatches.get(i / 2);
                int originalIndex = updatedWinners.indexOf(sourceMatch);

                if (originalIndex >= 0) {
                    updatedWinners.set(originalIndex, sourceMatch.toBuilder()
                            .loserNextMatch(targetMatch)
                            .build());
                }
            }

            winnersMatches.clear();
            winnersMatches.addAll(updatedWinners);
        }
    }

    private List<Match> sortMatchesForPersistence(Event event, Map<UUID, List<Match>> matchesByDraw) {
        if (event.getStages() == null) {
            return List.of();
        }

        List<Draw> draws = event.getStages().stream()
                .flatMap(stage -> stage.getDraws().stream())
                .toList();

        List<Match> sortedMatches = new java.util.ArrayList<>();
        draws.stream()
                .filter(draw -> DrawType.CONSOLATION.equals(draw.getDrawType()))
                .forEach(draw -> sortedMatches.addAll(sortMatchesByRoundDescending(matchesByDraw.get(draw.getId()))));
        draws.stream()
                .filter(draw -> !DrawType.CONSOLATION.equals(draw.getDrawType()))
                .forEach(draw -> sortedMatches.addAll(sortMatchesByRoundDescending(matchesByDraw.get(draw.getId()))));

        return sortedMatches;
    }

    private List<Match> sortMatchesByRoundDescending(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        return matches.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getRoundNumber() == null ? 0 : b.getRoundNumber(),
                        a.getRoundNumber() == null ? 0 : a.getRoundNumber()
                ))
                .toList();
    }

    private List<Match> firstRoundContestedMatches(List<Match> matches) {
        return firstRoundMatches(matches).stream()
                .filter(match -> match.getFirstInscriptionId() != null && match.getSecondInscriptionId() != null)
                .toList();
    }

    private List<Match> firstRoundMatches(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        return matches.stream()
                .filter(match -> match.getRoundNumber() != null && match.getRoundNumber() == 1)
                .toList();
    }

    private List<Match> scheduleGeneratedMatches(
            List<Match> matches,
            List<Court> courts,
            LocalDateTime firstMatchStart,
            java.util.concurrent.atomic.AtomicInteger scheduleIndex
    ) {
        if (matches == null || matches.isEmpty() || firstMatchStart == null || courts == null || courts.isEmpty()) {
            return matches;
        }

        Map<Integer, List<Match>> matchesByRound = matches.stream()
                .filter(match -> !isByeMatch(match))
                .collect(Collectors.groupingBy(
                        Match::getRoundNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (matchesByRound.isEmpty()) {
            return matches;
        }

        int timeSlot = 0;
        List<Match> result = new ArrayList<>();
        for (List<Match> roundMatches : matchesByRound.values()) {
            List<Match> remaining = new ArrayList<>(roundMatches);
            while (!remaining.isEmpty()) {
                Set<UUID> scheduledPlayers = new HashSet<>();
                List<Match> currentSlot = new ArrayList<>();
                List<Match> nextRemaining = new ArrayList<>();
                for (Match match : remaining) {
                    UUID first = match.getFirstInscriptionId();
                    UUID second = match.getSecondInscriptionId();
                    boolean firstBusy = first != null && scheduledPlayers.contains(first);
                    boolean secondBusy = second != null && scheduledPlayers.contains(second);
                    if (!firstBusy && !secondBusy) {
                        currentSlot.add(match);
                        if (first != null) scheduledPlayers.add(first);
                        if (second != null) scheduledPlayers.add(second);
                    } else {
                        nextRemaining.add(match);
                    }
                }
                if (currentSlot.isEmpty()) {
                    break;
                }
                int courtSlot = timeSlot;
                for (int i = 0; i < currentSlot.size(); i++) {
                    Match match = currentSlot.get(i);
                    Court court = courts.get(i % courts.size());
                    result.add(match.toBuilder()
                            .scheduledAt(firstMatchStart.plusHours(courtSlot))
                            .scheduleTimeType(courtSlot == 0 ? ScheduleTimeType.EXACT : ScheduleTimeType.NOT_BEFORE)
                            .courtId(court.getId())
                            .court(court.getName())
                            .build());
                }
                timeSlot++;
                remaining = nextRemaining;
            }
        }

        matches.stream()
                .filter(this::isByeMatch)
                .forEach(result::add);

        return result;
    }

    private boolean isByeMatch(Match match) {
        if (match == null || match.getWinnerId() == null) {
            return false;
        }

        return (match.getFirstInscriptionId() == null) != (match.getSecondInscriptionId() == null);
    }
}
