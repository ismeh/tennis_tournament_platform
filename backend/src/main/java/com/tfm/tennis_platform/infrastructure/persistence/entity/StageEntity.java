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
@Table(name = "stages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private EventEntity event;

    @Column(name = "stage_order", nullable = false)
    private Integer order;

    @Column(name = "stage_type")
    private String stageType;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DrawEntity> draws = new ArrayList<>();
}