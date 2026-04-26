package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.tfm.tennis_platform.domain.models.enums.EntryStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = true)
    private PersonEntity individualPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type")
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_status")
    private EntryStatus entryStatus;

    @Column(name = "seed")
    private Integer seed;

    @ManyToMany
    @JoinTable(
        name = "participant_persons",
        joinColumns = @JoinColumn(name = "participant_id"),
        inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    @Builder.Default
    private List<PersonEntity> members = new ArrayList<>();
}
