package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedule_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConfigEntity {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false, unique = true)
    private TournamentEntity tournament;

    @Column(name = "match_duration_minutes", nullable = false)
    @Builder.Default
    private int matchDurationMinutes = 60;

    @OneToMany(mappedBy = "scheduleConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimeSlotEntity> timeSlots = new ArrayList<>();
}
