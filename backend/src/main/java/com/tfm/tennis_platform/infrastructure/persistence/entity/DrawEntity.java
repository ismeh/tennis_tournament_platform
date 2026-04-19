package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "draws")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrawEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    @JsonIgnore
    private StageEntity stage;

    @Column(name = "draw_type", nullable = false)
    private String drawType;

    @Column(name = "draw_name")
    private String drawName;

    @OneToMany(mappedBy = "draw", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MatchupEntity> matchups = new ArrayList<>();
}