package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournaments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class TournamentEntity {
    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String formalName;

    @Column(name = "start_date", nullable = false)
    private LocalDate playStartDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate playEndDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "inscription_start_date")    
    private LocalDate inscriptionStartDate;

    @Column(name = "inscription_end_date")
    private LocalDate inscriptionEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Surface surface;

    private Integer maxPlayers;

    private String location;

    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Column(name = "location_place_id")
    private String locationPlaceId;

    @Column(name = "location_formatted_address", length = 500)
    private String locationFormattedAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Setter
    private TournamentStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private MemberEntity createdBy;

    @Column(name = "sets_per_match")
    private Integer setsPerMatch;

    @Column(name = "decisive_tiebreak_points")
    private Integer decisiveTiebreakPoints;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventEntity> events = new ArrayList<>();

    @Version
    private Long version;
}
