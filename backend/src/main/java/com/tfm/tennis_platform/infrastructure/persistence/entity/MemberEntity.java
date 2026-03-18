package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "person_id", updatable = false)
    private UUID personId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", updatable = false)
    private String passwordHash;

    @Column(name = "token_hash", length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    private MemberTier tier;

    @Column(name = "registered_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime registeredAt;
}