package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "sets", uniqueConstraints = @UniqueConstraint(columnNames = {"matchup_id", "set_number"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matchup_id", nullable = false)
    private MatchupEntity matchup;

    @Column(name = "set_number", nullable = false)
    private Short setNumber;

    @Column(name = "side1_games")
    private Integer side1Games;

    @Column(name = "side2_games")
    private Integer side2Games;

    @Column(name = "side1_tiebreak")
    private Integer side1Tiebreak;

    @Column(name = "side2_tiebreak")
    private Integer side2Tiebreak;
}