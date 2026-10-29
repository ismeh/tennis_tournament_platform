package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.events.TournamentUpdateEvent;

public interface TournamentUpdatePublisher {
    void publish(TournamentUpdateEvent event);
}
