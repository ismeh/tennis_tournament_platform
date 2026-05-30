package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.application.commands.EventCommand;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(exclude = "stages")
@Builder(builderClassName = "EventBuilder", buildMethodName = "buildInternal", toBuilder = true)
public class Event {
    private UUID id;
    private UUID tournamentId;
    private String gender;
    private Integer categoryId;
    @Singular
    private List<Stage> stages;

    public static class EventBuilder {
        public Event build() {
            if (gender == null || gender.isEmpty()) {
                throw new IllegalArgumentException("El género del evento es obligatorio.");
            }

            String normalizedGender = gender.trim().toUpperCase();
            if (!normalizedGender.equals("MALE") && !normalizedGender.equals("FEMALE") && !normalizedGender.equals("MIXED")) {
                throw new IllegalArgumentException("El género debe ser masculino, femenino o mixto.");
            }

            if (categoryId == null || categoryId < 0) {
                throw new IllegalArgumentException("La categoría del evento es obligatoria.");
            }

            this.gender = normalizedGender;

            if (id == null) {
                this.id = UUID.randomUUID();
            }
            if (stages == null) {
                this.stages = new ArrayList<>();
            }
            return buildInternal();
        }
    }

    public static Event createOrUpdateEvent(UUID tournamentId, EventCommand.EventItem eventItem, List<Stage> stages) {
        UUID eventId = eventItem.id() != null ? eventItem.id() : UUID.randomUUID();

        return Event.builder()
                .id(eventId)
                .tournamentId(tournamentId)
                .categoryId(eventItem.categoryId())
                .gender(eventItem.gender())
                .stages(stages)
                .build();
    }

    public static String buildEventName(EventCommand.EventItem eventItem) {
        return "Evento_%d_%s".formatted(eventItem.categoryId(), eventItem.gender());
    }
}
