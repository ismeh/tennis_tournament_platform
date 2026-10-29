package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PlayerInvitationRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteParticipantServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JpaParticipantRepository participantRepository;
    @Mock
    private PlayerInvitationRepository playerInvitationRepository;
    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private InviteParticipantService inviteParticipantService;

    private UUID tournamentId;
    private UUID participantId;
    private String requesterEmail;
    private Tournament tournament;
    private ParticipantEntity participantEntity;
    private TournamentEntity tournamentEntity;

    @BeforeEach
    void setUp() {
        tournamentId = UUID.randomUUID();
        participantId = UUID.randomUUID();
        requesterEmail = "organizer@test.com";

        tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Test")
                .playPeriod(new com.tfm.tennis_platform.domain.models.TournamentPeriod(java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(5)))
                .inscriptionPeriod(new com.tfm.tennis_platform.domain.models.TournamentPeriod(java.time.LocalDate.now().minusDays(5), java.time.LocalDate.now().minusDays(1)))
                .surface(com.tfm.tennis_platform.domain.models.enums.Surface.HARD)
                .maxPlayers(32)
                .location("Club de Tenis")
                .build();

        tournamentEntity = TournamentEntity.builder()
                .id(tournamentId)
                .formalName("Open Test")
                .build();

        participantEntity = ParticipantEntity.builder()
                .id(participantId)
                .tournament(tournamentEntity)
                .participantSource(ParticipantSource.MANUAL)
                .build();

        ReflectionTestUtils.setField(inviteParticipantService, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    void generateInvitationUrl_success() {
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participantEntity));
        when(playerInvitationRepository.findByParticipantId(participantId)).thenReturn(Optional.empty());

        String url = inviteParticipantService.generateInvitationUrl(tournamentId, participantId, requesterEmail);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:4200/aceptar-invitacion?token="));
        verify(tournamentService).assertTournamentAdmin(tournament, requesterEmail);
        verify(playerInvitationRepository).save(any(PlayerInvitation.class));
    }

    @Test
    void generateInvitationUrl_fails_when_participant_not_manual() {
        participantEntity.setParticipantSource(ParticipantSource.EXISTING_PERSON);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participantEntity));

        assertThrows(InvalidArgumentException.class, () ->
                inviteParticipantService.generateInvitationUrl(tournamentId, participantId, requesterEmail)
        );
        verify(playerInvitationRepository, never()).save(any());
    }

    @Test
    void generateInvitationUrl_fails_when_participant_belongs_to_other_tournament() {
        TournamentEntity otherTournament = TournamentEntity.builder().id(UUID.randomUUID()).build();
        participantEntity.setTournament(otherTournament);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participantEntity));

        assertThrows(InvalidArgumentException.class, () ->
                inviteParticipantService.generateInvitationUrl(tournamentId, participantId, requesterEmail)
        );
    }
}
