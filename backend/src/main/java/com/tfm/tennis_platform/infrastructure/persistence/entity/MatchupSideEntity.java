package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "matchup_sides", uniqueConstraints = @UniqueConstraint(columnNames = {"matchup_id", "side_number"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupSideEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matchup_id", nullable = false)
    private MatchupEntity matchup;

    @Column(name = "side_number", nullable = false)
    private Short sideNumber;

    @Column(name = "participant_id")
    private UUID participantId;
}