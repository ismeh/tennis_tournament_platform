package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ExpiredTokenException;
import com.tfm.tennis_platform.domain.exceptions.InvalidTokenException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.exceptions.UnauthorizedException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.models.PlayerInvitation;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import com.tfm.tennis_platform.domain.port.out.PlayerInvitationRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ClaimInvitationService {

    private final PlayerInvitationRepository playerInvitationRepository;
    private final MemberRepository memberRepository;
    private final PersonRepository personRepository;
    private final JpaParticipantRepository participantRepository;
    private final JpaPersonRepository personJpaRepository;

    @Transactional
    public void claimInvitation(String rawToken, String memberEmail) {
        String tokenHash = hashToken(rawToken);

        PlayerInvitation invitation = playerInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("El enlace de invitación no es válido."));

        if (invitation.isClaimed()) {
            throw new InvalidTokenException("Este enlace de invitación ya fue utilizado.");
        }

        if (invitation.isExpired()) {
            throw new ExpiredTokenException("El enlace de invitación ha caducado. Solicita uno nuevo al organizador.");
        }

        Member member = memberRepository.findByEmailWithPersonId(memberEmail)
                .orElseThrow(() -> new UnauthorizedException("No se pudo cargar tu perfil. Inicia sesión de nuevo."));

        ParticipantEntity participant = participantRepository.findById(invitation.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el participante de la invitación."));

        PersonEntity personEntity = resolvePersonEntity(member, participant);
        participant.setIndividualPerson(personEntity);
        participant.setParticipantSource(ParticipantSource.EXISTING_PERSON);
        participantRepository.save(participant);

        playerInvitationRepository.markAsClaimed(invitation.getId(), member.getId(), LocalDateTime.now());
    }

    public InvitationPreview previewInvitation(String rawToken) {
        String tokenHash = hashToken(rawToken);

        PlayerInvitation invitation = playerInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("El enlace de invitación no es válido."));

        ParticipantEntity participant = participantRepository.findById(invitation.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el participante de la invitación."));

        String tournamentName = participant.getTournament() != null
                ? participant.getTournament().getFormalName()
                : "Torneo desconocido";

        String playerDisplayName = buildDisplayName(participant);

        return new InvitationPreview(
                tournamentName,
                playerDisplayName,
                invitation.isExpired(),
                invitation.isClaimed()
        );
    }

    private PersonEntity resolvePersonEntity(Member member, ParticipantEntity participant) {
        if (member.getPersonId() != null) {
            return personJpaRepository.findById(member.getPersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el perfil del jugador."));
        }

        Person newPerson = Person.builder()
                .firstName(participant.getDisplayFirstName() != null ? participant.getDisplayFirstName() : "Jugador")
                .lastName(participant.getDisplayLastName())
                .gender(participant.getDisplayGender())
                .birthDate(participant.getDisplayBirthDate())
                .nationality(participant.getDisplayNationality())
                .tennisId(participant.getDisplayTennisId())
                .build();

        Person savedPerson = personRepository.save(newPerson);
        memberRepository.updatePersonId(member.getId(), savedPerson.getId());

        return personJpaRepository.findById(savedPerson.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo crear el perfil del jugador."));
    }

    private String buildDisplayName(ParticipantEntity participant) {
        String firstName = participant.getDisplayFirstName() != null ? participant.getDisplayFirstName() : "";
        String lastName = participant.getDisplayLastName() != null ? participant.getDisplayLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? "Jugador" : fullName;
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

    public record InvitationPreview(
            String tournamentName,
            String playerDisplayName,
            boolean expired,
            boolean claimed
    ) {}
}
