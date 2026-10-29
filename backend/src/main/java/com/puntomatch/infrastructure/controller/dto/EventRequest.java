package com.puntomatch.infrastructure.controller.dto;

import java.util.List;

/**
 * Payload for {@code POST /api/tournaments/{tournamentId}/events}.
 *
 * <p>Mirrors {@code TournamentEventsConfigRequest} on the Angular side:
 * {@code { "events": [{ "categoryId": 1, "gender": "MALE" }] }}.
 */
public record EventRequest(List<EventItem> events) {

    public EventRequest {
        events = events == null ? List.of() : List.copyOf(events);
    }

    public List<EventItem> getEvents() {
        return events;
    }

    public record EventItem(Integer categoryId, String gender) {

        public Integer getCategoryId() {
            return categoryId;
        }

        public String getGender() {
            return gender;
        }
    }
}
