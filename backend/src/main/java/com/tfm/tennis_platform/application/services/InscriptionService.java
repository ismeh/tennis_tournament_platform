package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.enums.EntryStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantType;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionCategoryCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionEventResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionGenderCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionPlayerResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionsResponse;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public TournamentInscriptionsResponse findByTournament(UUID tournamentId, UUID eventId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new IllegalArgumentException("Tournament not found");
        }

        if (eventId != null) {
            eventRepository.findByIdAndTournament_Id(eventId, tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found for tournament"));
        }

        List<EventEntity> tournamentEvents = eventRepository.findAllByTournamentId(tournamentId);
        List<TournamentInscriptionEventResponse> eventCatalog = tournamentEvents.stream()
                .map(this::toTournamentEventResponse)
                .toList();

        Map<Integer, CategoryCounterAccumulator> countersByCategory = initializeCategoryCounters(tournamentEvents, eventId);
        List<InscriptionEntity> inscriptions = eventId == null
                ? inscriptionRepository.findDetailedByTournamentId(tournamentId)
                : inscriptionRepository.findDetailedByTournamentIdAndEventId(tournamentId, eventId);

        List<TournamentInscriptionPlayerResponse> players = inscriptions.stream()
                .sorted(Comparator.comparing(InscriptionEntity::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .flatMap(inscription -> toPlayers(inscription).stream())
                .peek(player -> accumulateCounter(countersByCategory, player))
                .toList();

        List<TournamentInscriptionCategoryCountResponse> categoryCounts = countersByCategory.values().stream()
                .map(CategoryCounterAccumulator::toResponse)
                .toList();

        return new TournamentInscriptionsResponse(tournamentId, eventId, eventCatalog, categoryCounts, players);
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

    private TournamentInscriptionEventResponse toTournamentEventResponse(EventEntity event) {
        return new TournamentInscriptionEventResponse(
                event.getId(),
                event.getAgeCategory() != null ? event.getAgeCategory().getId() : null,
                getCategoryLabel(event.getAgeCategory()),
                getEventName(event),
                normalizeGender(event.getGender())
        );
    }

    private List<TournamentInscriptionPlayerResponse> toPlayers(InscriptionEntity inscription) {
        EventEntity event = inscription.getEvent();
        ParticipantEntity participant = inscription.getParticipant();
        List<PersonEntity> people = getVisiblePeople(participant);

        return people.stream()
                .map(person -> new TournamentInscriptionPlayerResponse(
                        inscription.getId(),
                        event != null ? event.getId() : null,
                        event != null && event.getAgeCategory() != null ? event.getAgeCategory().getId() : null,
                        event != null ? getCategoryLabel(event.getAgeCategory()) : null,
                        event != null ? getEventName(event) : null,
                        event != null ? normalizeGender(event.getGender()) : null,
                        person.getFirstName(),
                        person.getLastName(),
                        normalizeGender(person.getGender())
                ))
                .toList();
    }

    private List<PersonEntity> getVisiblePeople(ParticipantEntity participant) {
        if (participant == null) {
            return List.of();
        }

        if (participant.getParticipantType() == ParticipantType.INDIVIDUAL && participant.getIndividualPerson() != null) {
            return List.of(participant.getIndividualPerson());
        }

        if (participant.getMembers() == null || participant.getMembers().isEmpty()) {
            return List.of();
        }

        return participant.getMembers();
    }

    private Map<Integer, CategoryCounterAccumulator> initializeCategoryCounters(List<EventEntity> events, UUID eventId) {
        Map<Integer, CategoryCounterAccumulator> counters = new LinkedHashMap<>();

        for (EventEntity event : events) {
            if (eventId != null && !eventId.equals(event.getId())) {
                continue;
            }

            RefAgeCategoryEntity ageCategory = event.getAgeCategory();
            if (ageCategory == null || ageCategory.getId() == null) {
                continue;
            }

            counters.putIfAbsent(
                    ageCategory.getId(),
                    new CategoryCounterAccumulator(ageCategory.getId(), getCategoryLabel(ageCategory))
            );
        }

        return counters;
    }

    private void accumulateCounter(
            Map<Integer, CategoryCounterAccumulator> countersByCategory,
            TournamentInscriptionPlayerResponse player
    ) {
        if (player.categoryId() == null) {
            return;
        }

        countersByCategory
                .computeIfAbsent(player.categoryId(), categoryId -> new CategoryCounterAccumulator(categoryId, player.category()))
                .addPlayer(player.gender());
    }

    private String getCategoryLabel(RefAgeCategoryEntity ageCategory) {
        return ageCategory != null ? ageCategory.getCategory() : null;
    }

    private String getEventName(EventEntity event) {
        if (event == null) {
            return null;
        }

        if (!isBlank(event.getName())) {
            return event.getName();
        }

        String category = getCategoryLabel(event.getAgeCategory());
        String gender = toGenderLabel(event.getGender());
        if (category == null) {
            return gender;
        }
        if (gender == null) {
            return category;
        }

        return category + " - " + gender;
    }

    private String normalizeGender(String gender) {
        if (isBlank(gender)) {
            return "UNKNOWN";
        }

        return gender.trim().toUpperCase(Locale.ROOT);
    }

    private String toGenderLabel(String gender) {
        return switch (normalizeGender(gender)) {
            case "MALE" -> "Masculino";
            case "FEMALE" -> "Femenino";
            case "MIXED" -> "Mixto";
            default -> null;
        };
    }

    private static final class CategoryCounterAccumulator {
        private final Integer categoryId;
        private final String category;
        private long totalPlayers;
        private final Map<String, Long> totalsByGender = new LinkedHashMap<>();

        private CategoryCounterAccumulator(Integer categoryId, String category) {
            this.categoryId = categoryId;
            this.category = category;
            this.totalsByGender.put("MALE", 0L);
            this.totalsByGender.put("FEMALE", 0L);
            this.totalsByGender.put("MIXED", 0L);
            this.totalsByGender.put("UNKNOWN", 0L);
        }

        private void addPlayer(String gender) {
            String normalizedGender = gender == null ? "UNKNOWN" : gender;
            this.totalPlayers += 1;
            this.totalsByGender.compute(normalizedGender, (key, current) -> current == null ? 1L : current + 1L);
        }

        private TournamentInscriptionCategoryCountResponse toResponse() {
            List<TournamentInscriptionGenderCountResponse> genders = new ArrayList<>();
            this.totalsByGender.forEach((gender, totalPlayersByGender) ->
                    genders.add(new TournamentInscriptionGenderCountResponse(gender, totalPlayersByGender))
            );

            return new TournamentInscriptionCategoryCountResponse(categoryId, category, totalPlayers, genders);
        }
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
