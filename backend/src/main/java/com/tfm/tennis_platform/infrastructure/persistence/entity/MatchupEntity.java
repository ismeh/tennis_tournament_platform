package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matchups")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_id", nullable = false)
    private DrawEntity draw;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "match_number")
    private Integer matchNumber;

    @Column(name = "match_format")
    private String matchFormat;

    @Column(name = "status")
    private String status;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "court")
    private String court;

    @Column(name = "winner_side")
    private Short winnerSide;

    @OneToMany(mappedBy = "matchup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MatchupSideEntity> sides = new ArrayList<>();

    @OneToMany(mappedBy = "matchup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MatchSetEntity> sets = new ArrayList<>();
}