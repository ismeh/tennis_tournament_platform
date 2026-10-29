package com.tfm.tennis_platform.domain.models;

import java.time.LocalDate;
import java.util.Objects;

public record TournamentPeriod(LocalDate startDate, LocalDate endDate) {
    public TournamentPeriod {
        Objects.requireNonNull(startDate, "La fecha de inicio es obligatoria");
        Objects.requireNonNull(endDate, "La fecha de fin es obligatoria");
        if (endDate.isBefore(startDate))
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlaps(TournamentPeriod other) {
        return !this.endDate.isBefore(other.startDate) &&
                !other.endDate.isBefore(this.startDate);
    }
}
