package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true, builderClassName = "TournamentBuilder", buildMethodName = "buildInternal")
@ToString(exclude = "events")
@EqualsAndHashCode(exclude = "events")
public class Tournament {
    private final UUID id;
    private final String name;
    private final TournamentPeriod playPeriod;
    private final TournamentPeriod inscriptionPeriod;
    private final Surface surface;
    private final Integer maxPlayers;
    private final String location;
    private final TournamentStatus state; // 'soon', 'inscription', 'playing', 'finished'
    private final Member createdBy;
    @Singular
    private final List<Event> events;

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
            if (this.events == null) {
                this.events = new ArrayList<>();
            }

            // Validate there are no duplicated events by \(categoryId, gender\)
            var seen = new HashSet<String>();
            for (Event e : this.events) {
                String key = e.getCategoryId() + "|" + e.getGender();
                if (!seen.add(key)) {
                    throw new IllegalArgumentException("Duplicate event tuple (categoryId + gender)");
                }
            }
            return buildInternal();
        }
    }

    /**
     * Añade una lista de eventos al torneo, ignorando los que tengan un categoryId duplicado con los ya existentes en el torneo.
     * Devuelve un nuevo objeto Tournament con la lista de eventos actualizada.
     */
    public Tournament addEvent(List<Event> newEvents) {
        Objects.requireNonNull(newEvents, "newEvents must not be null");
        Set<Integer> existingIds = this.events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());
        List<Event> filteredNewEvents = newEvents.stream()
                .filter(e -> !existingIds.contains(e.getCategoryId()))
                .toList();
        List<Event> updatedEvents = new java.util.ArrayList<>(this.events);
        updatedEvents.addAll(filteredNewEvents);
        return this.toBuilder()
            .clearEvents()
                .events(updatedEvents)
                .build();
    }
}
