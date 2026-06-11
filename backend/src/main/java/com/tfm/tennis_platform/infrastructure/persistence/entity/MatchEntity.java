package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_id", nullable = false)
    private DrawEntity draw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_inscription_id")
    private InscriptionEntity firstInscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "second_inscription_id")
    private InscriptionEntity secondInscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private InscriptionEntity winner;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "bracket_position")
    private Integer bracketPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private MatchEntity nextMatch;

    public void setNextMatch(MatchEntity nextMatch) {
        this.nextMatch = nextMatch;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_next_match_id")
    private MatchEntity loserNextMatch;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type")
    private com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType scheduleTimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id")
    private CourtEntity courtResource;

    @Column(name = "court")
    private String court;

    @Column(name = "result")
    private String result;

    @Version
    @Column(name = "version")
    private Long version;
}
