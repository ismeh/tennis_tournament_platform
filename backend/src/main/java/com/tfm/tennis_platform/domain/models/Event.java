package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(exclude = "stages")
@Builder(builderClassName = "EventBuilder", buildMethodName = "buildInternal")
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
            if (stages == null) {
                this.stages = new ArrayList<>();
            }
            return buildInternal();
        }
    }
}