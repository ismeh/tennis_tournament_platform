package com.tfm.tennis_platform.infrastructure.persistence.entity;

import com.tfm.tennis_platform.domain.models.enums.EntryStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
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
    @Column(name = "participant_source")
    private ParticipantSource participantSource;

    @Column(name = "display_first_name")
    private String displayFirstName;

    @Column(name = "display_last_name")
    private String displayLastName;

    @Column(name = "display_gender")
    private String displayGender;

    @Column(name = "display_birth_date")
    private java.time.LocalDate displayBirthDate;

    @Column(name = "display_nationality")
    private String displayNationality;

    @Column(name = "display_tennis_id")
    private String displayTennisId;

    @Column(name = "display_club")
    private String displayClub;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type")
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_status")
    private EntryStatus entryStatus;

    @Column(name = "seed")
    private Integer seed;

    @Column(name = "points")
    private Integer points;

    @ManyToMany
    @JoinTable(
        name = "participant_persons",
        joinColumns = @JoinColumn(name = "participant_id"),
        inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    @Builder.Default
    private List<PersonEntity> members = new ArrayList<>();
}
