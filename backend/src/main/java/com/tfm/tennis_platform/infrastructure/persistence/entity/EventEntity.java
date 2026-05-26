package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_category_id")
    @Setter
    private RefAgeCategoryEntity ageCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnore
    @Setter
    private TournamentEntity tournament;

    private String name;

    private String discipline;

    @Column(name = "event_type")
    private String eventType;

    @Setter
    private String gender;


    @Column(name = "draw_size")
    private Integer drawSize;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StageEntity> stages = new ArrayList<>();
}