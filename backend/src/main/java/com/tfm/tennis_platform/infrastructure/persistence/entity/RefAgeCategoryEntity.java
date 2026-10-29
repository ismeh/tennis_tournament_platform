package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ref_age_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefAgeCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(length = 20)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "organizer_id")
    private UUID organizerId;
}
