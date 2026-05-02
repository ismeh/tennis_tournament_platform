package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(builderClassName = "EventBuilder", buildMethodName = "buildInternal")
public class Event {
    private UUID id;
    private UUID tournamentId;
    private String gender;
    private Integer categoryId;

    public static class EventBuilder {
        public Event build() {
            if (gender == null || gender.isEmpty()) {
                throw new IllegalArgumentException("gender is null or empty");
            }

            String normalizedGender = gender.trim().toUpperCase();
            if (!normalizedGender.equals("MALE") && !normalizedGender.equals("FEMALE") && !normalizedGender.equals("MIXED")) {
                throw new IllegalArgumentException("gender must be 'MALE', 'FEMALE' or 'MIXED'");
            }

            if (categoryId == null || categoryId < 0) {
                throw new IllegalArgumentException("categoryId is null or negative");
            }

            this.gender = normalizedGender;

            if (id == null) {
                this.id = UUID.randomUUID();
            }
            return buildInternal();
        }
    }
}