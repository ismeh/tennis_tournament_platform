package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private TournamentEntity tournament;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_id")
    private DrawEntity draw;

    @ManyToOne
    @JoinColumn(name = "first_inscription_id")
    private InscriptionEntity firstInscription;

    @ManyToOne
    @JoinColumn(name = "second_inscription_id")
    private InscriptionEntity secondInscription;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private InscriptionEntity winner;

    private Integer roundNumber;

    @ManyToOne
    @JoinColumn(name = "next_match_id")
    private MatchEntity nextMatch;

    private LocalDateTime scheduledAt;
    private String court;
    private String result;
}