package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "match_sets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSetEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(name = "first_player_games", nullable = false)
    private Integer firstPlayerGames;

    @Column(name = "second_player_games", nullable = false)
    private Integer secondPlayerGames;

    @Column(name = "first_player_tiebreak")
    private Integer firstPlayerTiebreak;

    @Column(name = "second_player_tiebreak")
    private Integer secondPlayerTiebreak;
}
