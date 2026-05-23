package com.tfm.tennis_platform.infrastructure.controller.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class EventRequest {
    private List<EventCategoryGender> events;

    @Data
    public static class EventCategoryGender {
        private UUID id;
        private Integer categoryId;
        private String gender;
        private List<String> stages;
    }
}
