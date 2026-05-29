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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.time.LocalTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true, builderClassName = "TournamentBuilder", buildMethodName = "buildInternal")
@ToString(exclude = "events")
@EqualsAndHashCode(exclude = "events")
public class Tournament {
    private final UUID id;
    private final String name;
    private final TournamentPeriod playPeriod;
    private final LocalTime startTime;
    private final TournamentPeriod inscriptionPeriod;
    private final Surface surface;
    private final Integer maxPlayers;
    private final String location;
    private final TournamentStatus state; // 'soon', 'inscription', 'playing', 'finished'
    private final Member createdBy;
    private final List<Event> events;

    public static class TournamentBuilder {
        public Tournament build() {
            Objects.requireNonNull(name, "El nombre del torneo es obligatorio.");
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del torneo es obligatorio.");
            }
            Objects.requireNonNull(playPeriod, "Las fechas de juego son obligatorias.");
            Objects.requireNonNull(inscriptionPeriod, "Las fechas de inscripción son obligatorias.");
            Objects.requireNonNull(surface, "La superficie del torneo es obligatoria.");
            Objects.requireNonNull(maxPlayers, "El número máximo de jugadores es obligatorio.");
            if (maxPlayers <= 0) {
                throw new IllegalArgumentException("El número máximo de jugadores debe ser mayor que cero.");
            }
            Objects.requireNonNull(location, "La ubicación del torneo es obligatoria.");
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
                    throw new IllegalArgumentException("No puedes repetir la misma categoría y género en un torneo.");
                }
            }
            return buildInternal();
        }
    }

    public Tournament setEvents(List<Event> newEvents) {
        Objects.requireNonNull(newEvents, "Debes enviar al menos un evento válido.");
        return this.toBuilder()
                .events(newEvents)
                .build();
    }
}
