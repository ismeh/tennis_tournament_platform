package com.tfm.tennis_platform.application.dto;

import java.util.List;

public record EventCommand(List<EventItem> events) {
    public record EventItem(
            Integer categoryId,
            String gender,
            List<String> stages
    ) {
    }
}
