package com.tfm.tennis_platform.infrastructure.controller.dto;

import lombok.Data;
import java.util.List;

@Data
public class EventRequest {
    private List<EventCategoryGender> events;

    @Data
    public static class EventCategoryGender {
        private Integer categoryId;
        private String gender;
    }
}
