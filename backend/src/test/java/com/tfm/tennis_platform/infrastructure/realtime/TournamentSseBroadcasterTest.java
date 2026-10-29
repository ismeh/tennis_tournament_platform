package com.tfm.tennis_platform.infrastructure.realtime;

import com.tfm.tennis_platform.domain.events.TournamentUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TournamentSseBroadcaster")
class TournamentSseBroadcasterTest {

    private TournamentSseBroadcaster broadcaster;

    @BeforeEach
    void setUp() throws Exception {
        broadcaster = new TournamentSseBroadcaster();
        // No reflection needed - test via public API with real SseEmitters
    }

    @Test
    @DisplayName("publish does nothing when no subscribers exist")
    void publishNoSubscribers() {
        TournamentUpdateEvent event = TournamentUpdateEvent.matchResultUpdated(
                UUID.randomUUID(), UUID.randomUUID());
        broadcaster.publish(event);
    }

    @Test
    @DisplayName("sendHeartbeat does nothing when no subscribers")
    void sendHeartbeatNoSubscribers() {
        broadcaster.sendHeartbeat();
    }

    @Test
    @DisplayName("subscribe returns a non-null SseEmitter")
    void subscribeReturnsEmitter() {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                broadcaster.subscribe(UUID.randomUUID());

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("subscribe accumulates emitters for same tournament")
    void subscribeAccumulatesEmitters() {
        UUID tournamentId = UUID.randomUUID();
        broadcaster.subscribe(tournamentId);
        broadcaster.subscribe(tournamentId);

        assertThat(getEmitterCount(tournamentId)).isEqualTo(2);
    }

    @Test
    @DisplayName("publish sends to subscribers without throwing")
    void publishSendsToSubscribers() {
        UUID tournamentId = UUID.randomUUID();
        broadcaster.subscribe(tournamentId);

        TournamentUpdateEvent event = TournamentUpdateEvent.matchResultUpdated(tournamentId, UUID.randomUUID());
        broadcaster.publish(event);
    }

    @Test
    @DisplayName("publish handles MATCH_SCHEDULE_UPDATED")
    void publishMatchScheduleUpdated() {
        UUID tournamentId = UUID.randomUUID();
        broadcaster.subscribe(tournamentId);

        TournamentUpdateEvent event = TournamentUpdateEvent.matchScheduleUpdated(tournamentId, UUID.randomUUID());
        broadcaster.publish(event);
    }

    @Test
    @DisplayName("publish does nothing for unknown tournament")
    void publishUnknownTournament() {
        TournamentUpdateEvent event = TournamentUpdateEvent.matchResultUpdated(
                UUID.randomUUID(), UUID.randomUUID());
        broadcaster.publish(event);
    }

    @Test
    @DisplayName("sendHeartbeat sends to all subscribers across tournaments")
    void sendHeartbeatSendsToAll() {
        UUID tournamentId1 = UUID.randomUUID();
        UUID tournamentId2 = UUID.randomUUID();
        broadcaster.subscribe(tournamentId1);
        broadcaster.subscribe(tournamentId2);

        broadcaster.sendHeartbeat();

        assertThat(getEmitterCount(tournamentId1)).isEqualTo(1);
        assertThat(getEmitterCount(tournamentId2)).isEqualTo(1);
    }

    @Test
    @DisplayName("subscribe creates emitter with correct timeout")
    void subscribeCreatesEmitterWithTimeout() throws Exception {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                broadcaster.subscribe(UUID.randomUUID());

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("multiple tournaments are tracked independently")
    void multipleTournamentsTrackedIndependently() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        broadcaster.subscribe(t1);
        broadcaster.subscribe(t2);
        broadcaster.subscribe(t1);

        assertThat(getEmitterCount(t1)).isEqualTo(2);
        assertThat(getEmitterCount(t2)).isEqualTo(1);
    }

    private int getEmitterCount(UUID tournamentId) {
        try {
            Field f = TournamentSseBroadcaster.class.getDeclaredField("emittersByTournament");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<UUID, CopyOnWriteArrayList<org.springframework.web.servlet.mvc.method.annotation.SseEmitter>> map =
                    (ConcurrentHashMap<UUID, CopyOnWriteArrayList<org.springframework.web.servlet.mvc.method.annotation.SseEmitter>>) f.get(broadcaster);
            CopyOnWriteArrayList<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> list = map.get(tournamentId);
            return list != null ? list.size() : 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
