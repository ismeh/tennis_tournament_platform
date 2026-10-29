package com.tfm.tennis_platform.infrastructure.realtime;

import com.tfm.tennis_platform.domain.events.TournamentUpdateEvent;
import com.tfm.tennis_platform.domain.port.out.TournamentUpdatePublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TournamentSseBroadcaster implements TournamentUpdatePublisher {

    private static final long EMITTER_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByTournament = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID tournamentId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        emittersByTournament.computeIfAbsent(tournamentId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tournamentId, emitter));
        emitter.onTimeout(() -> removeEmitter(tournamentId, emitter));
        emitter.onError(ignored -> removeEmitter(tournamentId, emitter));

        send(tournamentId, emitter, "connected", new TournamentSseConnectionEvent(tournamentId, LocalDateTime.now()));
        return emitter;
    }

    @Override
    public void publish(TournamentUpdateEvent event) {
        List<SseEmitter> emitters = emittersByTournament.get(event.tournamentId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            send(event.tournamentId(), emitter, toSseName(event), event);
        }
    }

    @Scheduled(fixedRate = 25_000)
    public void sendHeartbeat() {
        emittersByTournament.forEach((tournamentId, emitters) -> {
            TournamentSseConnectionEvent event = new TournamentSseConnectionEvent(tournamentId, LocalDateTime.now());
            for (SseEmitter emitter : emitters) {
                send(tournamentId, emitter, "heartbeat", event);
            }
        });
    }

    private String toSseName(TournamentUpdateEvent event) {
        return switch (event.type()) {
            case MATCH_RESULT_UPDATED -> "match-result-updated";
            case MATCH_SCHEDULE_UPDATED -> "match-schedule-updated";
        };
    }

    private void send(UUID tournamentId, SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException ex) {
            removeEmitter(tournamentId, emitter);
        }
    }

    private void removeEmitter(UUID tournamentId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByTournament.get(tournamentId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByTournament.remove(tournamentId);
        }
    }

    private record TournamentSseConnectionEvent(UUID tournamentId, LocalDateTime occurredAt) {
    }
}
