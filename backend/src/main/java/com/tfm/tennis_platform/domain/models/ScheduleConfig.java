package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ScheduleConfig {
    private final UUID id;
    private final UUID tournamentId;
    @Builder.Default
    private final List<TimeSlot> timeSlots = new ArrayList<>();
    @Builder.Default
    private final int matchDurationMinutes = 60;
}
