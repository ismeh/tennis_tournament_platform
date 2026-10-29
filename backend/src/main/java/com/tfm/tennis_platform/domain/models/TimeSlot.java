package com.tfm.tennis_platform.domain.models;

import java.time.LocalTime;

public record TimeSlot(LocalTime startTime, LocalTime endTime) {
    public TimeSlot {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start and end time are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
    }
}
