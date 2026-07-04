package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PlayerInvitationRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteParticipantService {

    private final TournamentRepository tournamentRepository;
    private final MemberRepository memberRepository;
    private final JpaParticipantRepository participantRepository;
    private final PlayerInvitationRepository playerInvitationRepository;
    private final TournamentService tournamentService;

    @Value("${application.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateInvitationUrl(UUID tournamentId, UUID participantId, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        ParticipantEntity participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el participante solicitado."));

        if (!participant.getTournament().getId().equals(tournamentId)) {
            throw new InvalidArgumentException("El participante no pertenece a este torneo.");
        }

        if (participant.getParticipantSource() != ParticipantSource.MANUAL) {
            throw new InvalidArgumentException("Solo se puede invitar a jugadores añadidos manualmente.");
        }

        playerInvitationRepository.findByParticipantId(participantId).ifPresent(existing -> {
            if (existing.isClaimed()) {
                throw new InvalidArgumentException("Este jugador ya ha aceptado su invitación y tiene una cuenta vinculada.");
            }
        });

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(72);

        PlayerInvitation invitation = PlayerInvitation.builder()
                .participantId(participantId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();

        playerInvitationRepository.save(invitation);

        return buildInvitationUrl(rawToken);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String buildInvitationUrl(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return frontendBaseUrl + "/aceptar-invitacion?token=" + encodedToken;
    }
}
