package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscriptions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ParticipantEntity participant;

    @Column(name = "status")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payment_status")
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "registered_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime registeredAt;
}
