package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournaments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String formalName;

    @Column(name = "start_date", nullable = false)
    private LocalDate playStartDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate playEndDate;

    @Column(name = "inscription_start_date")
    private LocalDate inscriptionStartDate;

    @Column(name = "inscription_end_date")
    private LocalDate inscriptionEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Surface surface;

    private Integer maxPlayers;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TournamentStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private MemberEntity createdBy;

    @ManyToMany
    @JoinTable(
            name = "tournament_categories",
            joinColumns = @JoinColumn(name = "tournament_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CategoryEntity> categories;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventEntity> events = new ArrayList<>();
}