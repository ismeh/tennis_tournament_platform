package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder
public class Tournament {
    private final UUID id;
    private final String name;
    private final TournamentPeriod playPeriod;
    private final TournamentPeriod inscriptionPeriod;
    private final Surface surface;
    private final Integer maxPlayers;
    private final String location;
    private final TournamentStatus state; // 'soon', 'inscription', 'playing', 'finished'
    private final UUID createdBy; // ID of the member who created the tournament
    private final List<UUID> eventIds; // List of event IDs associated with this tournament

    public static class TournamentBuilder {
        public Tournament build() {
            Objects.requireNonNull(name, "name must not be null");
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("name must not be empty");
            }
            Objects.requireNonNull(playPeriod, "playPeriod must not be null");
            Objects.requireNonNull(inscriptionPeriod, "inscriptionPeriod must not be null");
            Objects.requireNonNull(surface, "surface must not be null");
            Objects.requireNonNull(maxPlayers, "maxPlayers must not be null");
            if (maxPlayers <= 0) {
                throw new IllegalArgumentException("maxPlayers must be greater than 0");
            }
            Objects.requireNonNull(location, "location must not be null");

            if (this.state == null) {
                this.state = TournamentStatus.DRAFT;
            }
            if (this.eventIds == null) {
                this.eventIds = List.of();
            }

            return new Tournament(id, name, playPeriod, inscriptionPeriod, surface, maxPlayers, location, state, createdBy, eventIds);
        }
    }
}
