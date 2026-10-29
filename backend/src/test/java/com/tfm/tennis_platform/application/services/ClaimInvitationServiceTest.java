package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ExpiredTokenException;
import com.tfm.tennis_platform.domain.exceptions.InvalidTokenException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import com.tfm.tennis_platform.domain.port.out.PlayerInvitationRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimInvitationServiceTest {

    @Mock
    private PlayerInvitationRepository playerInvitationRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private JpaParticipantRepository participantRepository;
    @Mock
    private JpaPersonRepository personJpaRepository;

    @InjectMocks
    private ClaimInvitationService claimInvitationService;

    private String rawToken;
    private String tokenHash;
    private String memberEmail;
    private Member member;
    private ParticipantEntity participant;
    private PlayerInvitation invitation;
    private TournamentEntity tournamentEntity;

    @BeforeEach
    void setUp() {
        rawToken = "testRawTokenStringForClaimInvitationServiceTest";
        tokenHash = hashToken(rawToken);
        memberEmail = "player@test.com";

        member = Member.builder()
                .id(UUID.randomUUID())
                .email(memberEmail)
                .build();

        tournamentEntity = TournamentEntity.builder()
                .id(UUID.randomUUID())
                .formalName("Tournament Name")
                .build();

        participant = ParticipantEntity.builder()
                .id(UUID.randomUUID())
                .tournament(tournamentEntity)
                .displayFirstName("Invited")
                .displayLastName("Player")
                .participantSource(ParticipantSource.MANUAL)
                .build();

        invitation = PlayerInvitation.builder()
                .id(UUID.randomUUID())
                .participantId(participant.getId())
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    @Test
    void previewInvitation_success() {
        when(playerInvitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));
        when(participantRepository.findById(invitation.getParticipantId())).thenReturn(Optional.of(participant));

        ClaimInvitationService.InvitationPreview preview = claimInvitationService.previewInvitation(rawToken);

        assertNotNull(preview);
        assertEquals("Tournament Name", preview.tournamentName());
        assertEquals("Invited Player", preview.playerDisplayName());
        assertFalse(preview.expired());
        assertFalse(preview.claimed());
    }

    @Test
    void claimInvitation_success_creates_person_and_updates_participant() {
        Person newPerson = Person.builder()
                .id(UUID.randomUUID())
                .firstName("Invited")
                .lastName("Player")
                .build();

        PersonEntity personEntity = PersonEntity.builder()
                .id(newPerson.getId())
                .firstName("Invited")
                .lastName("Player")
                .build();

        when(playerInvitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));
        when(memberRepository.findByEmailWithPersonId(memberEmail)).thenReturn(Optional.of(member));
        when(participantRepository.findById(invitation.getParticipantId())).thenReturn(Optional.of(participant));
        when(personRepository.save(any(Person.class))).thenReturn(newPerson);
        when(personJpaRepository.findById(newPerson.getId())).thenReturn(Optional.of(personEntity));

        claimInvitationService.claimInvitation(rawToken, memberEmail);

        verify(personRepository).save(any(Person.class));
        verify(memberRepository).updatePersonId(eq(member.getId()), eq(newPerson.getId()));
        verify(playerInvitationRepository).markAsClaimed(eq(invitation.getId()), eq(member.getId()), any());
        assertEquals(ParticipantSource.EXISTING_PERSON, participant.getParticipantSource());
        assertEquals(personEntity, participant.getIndividualPerson());
    }

    @Test
    void claimInvitation_fails_when_expired() {
        PlayerInvitation expiredInvitation = invitation.toBuilder()
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(playerInvitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredInvitation));

        assertThrows(ExpiredTokenException.class, () ->
                claimInvitationService.claimInvitation(rawToken, memberEmail)
        );
        verify(playerInvitationRepository, never()).markAsClaimed(any(), any(), any());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
