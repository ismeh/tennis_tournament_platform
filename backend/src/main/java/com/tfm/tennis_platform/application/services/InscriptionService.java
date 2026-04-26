package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.enums.EntryStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantType;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionResponse;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaEventRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaParticipantRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaPersonRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final JpaTournamentRepository tournamentRepository;
    private final JpaEventRepository eventRepository;
    private final JpaInscriptionRepository inscriptionRepository;
    private final JpaMemberRepository memberRepository;
    private final JpaPersonRepository personRepository;
    private final JpaParticipantRepository participantRepository;

    @Transactional
    public EventInscriptionResponse register(
            UUID tournamentId,
            UUID eventId,
            EventInscriptionRequest request,
            String requesterEmail
    ) {
        TournamentEntity tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new IllegalArgumentException("Las inscripciones solo estan permitidas con el torneo en estado OPEN");
        }

        EventEntity event = eventRepository.findByIdAndTournament_Id(eventId, tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for tournament"));

        MemberEntity member = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        validateProfileComplete(member);
        PersonEntity requesterPerson = personRepository.findById(member.getPersonId())
                .orElseThrow(() -> new IllegalStateException("Profile data not found"));

        ParticipantEntity participant;
        if (request.partnerId() == null) {
            // Singles inscription
            participant = getOrCreateIndividualParticipant(tournament, requesterPerson);
        } else {
            // Doubles inscription
            if (request.partnerId().equals(member.getId())) {
                throw new IllegalArgumentException("El partner no puede ser el mismo miembro solicitante");
            }
            MemberEntity partnerMember = memberRepository.findById(request.partnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Partner not found"));
            validateProfileComplete(partnerMember);
            PersonEntity partnerPerson = personRepository.findById(partnerMember.getPersonId())
                    .orElseThrow(() -> new IllegalStateException("Partner profile data not found"));

            participant = getOrCreatePairParticipant(tournament, requesterPerson, partnerPerson);
        }

        if (inscriptionRepository.existsByEvent_IdAndParticipant_Id(eventId, participant.getId())) {
            throw new IllegalStateException("Ya existe una inscripcion para este participante en este evento");
        }

        InscriptionEntity inscription = InscriptionEntity.builder()
                .event(event)
                .participant(participant)
                .status("PENDING")
                .paymentStatus("UNPAID")
                .build();

        InscriptionEntity saved = inscriptionRepository.save(inscription);
        return toResponse(saved);
    }

    private ParticipantEntity getOrCreateIndividualParticipant(TournamentEntity tournament, PersonEntity person) {
        return participantRepository.findByTournamentIdAndIndividualPersonId(tournament.getId(), person.getId())
                .orElseGet(() -> {
                    ParticipantEntity newParticipant = ParticipantEntity.builder()
                            .tournament(tournament)
                            .individualPerson(person)
                            .participantType(ParticipantType.INDIVIDUAL)
                            .entryStatus(EntryStatus.DIRECT_ACCEPTANCE)
                            .members(List.of(person))
                            .build();
                    return participantRepository.save(newParticipant);
                });
    }

    private ParticipantEntity getOrCreatePairParticipant(TournamentEntity tournament, PersonEntity person1, PersonEntity person2) {
        // Simple logic to find existing pair: find participants of type PAIR in this tournament and check members
        // For simplicity, let's look for a participant that has both persons in its members list.
        // This is not very efficient if there are many participants, but fine for now.
        return participantRepository.findByTournamentId(tournament.getId()).stream()
                .filter(p -> p.getParticipantType() == ParticipantType.PAIR)
                .filter(p -> p.getMembers().contains(person1) && p.getMembers().contains(person2))
                .findFirst()
                .orElseGet(() -> {
                    ParticipantEntity newParticipant = ParticipantEntity.builder()
                            .tournament(tournament)
                            .participantType(ParticipantType.PAIR)
                            .entryStatus(EntryStatus.DIRECT_ACCEPTANCE)
                            .members(List.of(person1, person2))
                            .build();
                    return participantRepository.save(newParticipant);
                });
    }

    @Transactional(readOnly = true)
    public List<EventInscriptionResponse> findByEvent(UUID tournamentId, UUID eventId) {
        eventRepository.findByIdAndTournament_Id(eventId, tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for tournament"));

        return inscriptionRepository.findByEvent_Id(eventId).stream()
                .sorted(Comparator.comparing(InscriptionEntity::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    private EventInscriptionResponse toResponse(InscriptionEntity entity) {
        return new EventInscriptionResponse(
                entity.getId(),
                entity.getEvent() != null ? entity.getEvent().getId() : null,
                entity.getParticipant() != null ? entity.getParticipant().getId() : null,
                entity.getStatus(),
                entity.getPaymentStatus(),
                entity.getRegisteredAt()
        );
    }

    private void validateProfileComplete(MemberEntity member) {
        if (member.getPersonId() == null) {
            throw new IllegalStateException("Debes completar tu perfil antes de inscribirte");
        }

        PersonEntity person = personRepository.findById(member.getPersonId())
                .orElseThrow(() -> new IllegalStateException("Debes completar tu perfil antes de inscribirte"));

        if (isBlank(person.getFirstName()) || isBlank(person.getGender()) || person.getBirthDate() == null) {
            throw new IllegalStateException("Debes completar tu perfil antes de inscribirte");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
