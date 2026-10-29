package com.puntomatch.application.commands;

import java.util.List;
import java.util.UUID;

public record EventCommand(List<EventItem> events) {
    public record EventItem(
            UUID id,
            Integer categoryId,
            String gender,
            List<String> stages
    ) {
    }
}
