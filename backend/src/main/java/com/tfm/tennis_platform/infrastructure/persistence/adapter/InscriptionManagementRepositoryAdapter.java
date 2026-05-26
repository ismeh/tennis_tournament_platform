package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionResult;
import com.tfm.tennis_platform.domain.models.inscription.ManualEventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionCategoryCount;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionEventView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionGenderCount;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionPlayerView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import com.tfm.tennis_platform.domain.models.enums.EntryStatus;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.ParticipantType;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.InscriptionManagementRepository;
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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InscriptionManagementRepositoryAdapter implements InscriptionManagementRepository {

    private final JpaTournamentRepository tournamentRepository;
    private final JpaEventRepository eventRepository;
    private final JpaInscriptionRepository inscriptionRepository;
    private final JpaMemberRepository memberRepository;
    private final JpaPersonRepository personRepository;
    private final JpaParticipantRepository participantRepository;

    @Override
    @Transactional
    public EventInscriptionResult register(UUID tournamentId, UUID eventId, EventInscriptionCommand request, String requesterEmail) {
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
            participant = getOrCreateIndividualParticipant(tournament, requesterPerson, ParticipantSource.EXISTING_PERSON);
        } else {
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
        return toResult(saved);
    }

    @Override
    @Transactional
    public EventInscriptionResult registerManual(UUID tournamentId, UUID eventId, ManualEventInscriptionCommand request, String requesterEmail) {
        TournamentEntity tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new IllegalArgumentException("Las inscripciones solo estan permitidas con el torneo en estado OPEN");
        }

        EventEntity event = eventRepository.findByIdAndTournament_Id(eventId, tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for tournament"));

        MemberEntity requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        validateTournamentOwner(tournament, requester);

        ParticipantEntity participant = resolveManualParticipant(tournament, request);

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
        return toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventInscriptionResult> findByEvent(UUID tournamentId, UUID eventId) {
        eventRepository.findByIdAndTournament_Id(eventId, tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found for tournament"));

        return inscriptionRepository.findByEvent_Id(eventId).stream()
                .sorted(Comparator.comparing(InscriptionEntity::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentInscriptionsView findByTournament(UUID tournamentId, UUID eventId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new IllegalArgumentException("Tournament not found");
        }

        if (eventId != null) {
            eventRepository.findByIdAndTournament_Id(eventId, tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found for tournament"));
        }

        List<EventEntity> tournamentEvents = eventRepository.findAllByTournamentId(tournamentId);
        List<TournamentInscriptionEventView> eventCatalog = tournamentEvents.stream()
                .map(this::toTournamentEventResponse)
                .toList();

        Map<Integer, CategoryCounterAccumulator> countersByCategory = initializeCategoryCounters(tournamentEvents, eventId);
        List<InscriptionEntity> inscriptions = eventId == null
                ? inscriptionRepository.findDetailedByTournamentId(tournamentId)
                : inscriptionRepository.findDetailedByTournamentIdAndEventId(tournamentId, eventId);

        List<TournamentInscriptionPlayerView> players = inscriptions.stream()
                .sorted(Comparator.comparing(InscriptionEntity::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .flatMap(inscription -> toPlayers(inscription).stream())
                .peek(player -> accumulateCounter(countersByCategory, player))
                .toList();

        List<TournamentInscriptionCategoryCount> categoryCounts = countersByCategory.values().stream()
                .map(CategoryCounterAccumulator::toResponse)
                .toList();

        return new TournamentInscriptionsView(tournamentId, eventId, eventCatalog, categoryCounts, players);
    }

    private EventInscriptionResult toResult(InscriptionEntity entity) {
        return new EventInscriptionResult(
                entity.getId(),
                entity.getEvent() != null ? entity.getEvent().getId() : null,
                entity.getParticipant() != null ? entity.getParticipant().getId() : null,
                entity.getStatus(),
                entity.getPaymentStatus(),
                entity.getRegisteredAt()
        );
    }

    private TournamentInscriptionEventView toTournamentEventResponse(EventEntity event) {
        return new TournamentInscriptionEventView(
                event.getId(),
                event.getAgeCategory() != null ? event.getAgeCategory().getId() : null,
                getCategoryLabel(event.getAgeCategory()),
                getEventName(event),
                normalizeGender(event.getGender())
        );
    }

    private List<TournamentInscriptionPlayerView> toPlayers(InscriptionEntity inscription) {
        EventEntity event = inscription.getEvent();
        ParticipantEntity participant = inscription.getParticipant();
        List<PersonEntity> people = getVisiblePeople(participant);
        ParticipantSource participantSource = resolveParticipantSource(participant);

        return people.stream()
                .map(person -> new TournamentInscriptionPlayerView(
                        inscription.getId(),
                        event != null ? event.getId() : null,
                        event != null && event.getAgeCategory() != null ? event.getAgeCategory().getId() : null,
                        event != null ? getCategoryLabel(event.getAgeCategory()) : null,
                        event != null ? getEventName(event) : null,
                        event != null ? normalizeGender(event.getGender()) : null,
                        person.getId(),
                        participantSource.name(),
                        person.getTennisId(),
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

        if (participant.getIndividualPerson() == null && !isBlank(participant.getDisplayFirstName())) {
            return List.of(PersonEntity.builder()
                    .firstName(participant.getDisplayFirstName())
                    .lastName(participant.getDisplayLastName())
                    .birthDate(participant.getDisplayBirthDate())
                    .nationality(participant.getDisplayNationality())
                    .gender(participant.getDisplayGender())
                    .tennisId(participant.getDisplayTennisId())
                    .build());
        }

        if (participant.getParticipantType() == ParticipantType.INDIVIDUAL && participant.getIndividualPerson() != null) {
            return List.of(participant.getIndividualPerson());
        }

        if (participant.getMembers() == null || participant.getMembers().isEmpty()) {
            return List.of();
        }

        return participant.getMembers();
    }

    private ParticipantSource resolveParticipantSource(ParticipantEntity participant) {
        if (participant == null) {
            return ParticipantSource.MANUAL;
        }

        if (participant.getParticipantSource() != null) {
            return participant.getParticipantSource();
        }

        if (participant.getIndividualPerson() != null || (participant.getMembers() != null && !participant.getMembers().isEmpty())) {
            return ParticipantSource.EXISTING_PERSON;
        }

        return ParticipantSource.MANUAL;
    }

    private ParticipantEntity resolveManualParticipant(TournamentEntity tournament, ManualEventInscriptionCommand request) {
        ParticipantSource participantSource = request.playerSource();
        if (participantSource == null) {
            throw new IllegalArgumentException("playerSource is required");
        }

        return switch (participantSource) {
            case EXISTING_PERSON -> resolveExistingPersonParticipant(tournament, request.personId());
            case MANUAL, PROFESSIONAL -> resolveManualSnapshotParticipant(tournament, request);
        };
    }

    private ParticipantEntity resolveExistingPersonParticipant(TournamentEntity tournament, UUID personId) {
        if (personId == null) {
            throw new IllegalArgumentException("personId is required for existing players");
        }

        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found"));

        return getOrCreateIndividualParticipant(tournament, person, ParticipantSource.EXISTING_PERSON);
    }

    private ParticipantEntity resolveManualSnapshotParticipant(TournamentEntity tournament, ManualEventInscriptionCommand request) {
        if (isBlank(request.firstName()) || isBlank(request.gender())) {
            throw new IllegalArgumentException("firstName and gender are required for manual players");
        }

        return participantRepository.findByTournamentId(tournament.getId()).stream()
                .filter(participant -> participant.getParticipantType() == ParticipantType.INDIVIDUAL)
                .filter(participant -> participant.getParticipantSource() == request.playerSource())
                .filter(participant -> equalsNormalized(participant.getDisplayFirstName(), request.firstName()))
                .filter(participant -> equalsNormalized(participant.getDisplayLastName(), request.lastName()))
                .filter(participant -> equalsNormalized(participant.getDisplayGender(), request.gender()))
                .filter(participant -> equalsNormalized(participant.getDisplayTennisId(), request.tennisId()))
                .findFirst()
                .orElseGet(() -> participantRepository.save(ParticipantEntity.builder()
                        .tournament(tournament)
                        .participantType(ParticipantType.INDIVIDUAL)
                        .participantSource(request.playerSource())
                        .entryStatus(EntryStatus.DIRECT_ACCEPTANCE)
                        .displayFirstName(request.firstName().trim())
                        .displayLastName(normalizeNullable(request.lastName()))
                        .displayGender(request.gender().trim().toUpperCase(Locale.ROOT))
                        .displayBirthDate(request.birthDate())
                        .displayNationality(normalizeNullable(request.nationality()))
                        .displayTennisId(normalizeNullable(request.tennisId()))
                        .members(List.of())
                        .build()));
    }

    private ParticipantEntity getOrCreateIndividualParticipant(TournamentEntity tournament, PersonEntity person, ParticipantSource participantSource) {
        return participantRepository.findByTournamentIdAndIndividualPersonId(tournament.getId(), person.getId())
                .orElseGet(() -> {
                    ParticipantEntity newParticipant = ParticipantEntity.builder()
                            .tournament(tournament)
                            .individualPerson(person)
                            .participantSource(participantSource)
                            .participantType(ParticipantType.INDIVIDUAL)
                            .entryStatus(EntryStatus.DIRECT_ACCEPTANCE)
                            .members(List.of(person))
                            .displayFirstName(person.getFirstName())
                            .displayLastName(person.getLastName())
                            .displayBirthDate(person.getBirthDate())
                            .displayNationality(person.getNationality())
                            .displayGender(person.getGender())
                            .displayTennisId(person.getTennisId())
                            .build();
                    return participantRepository.save(newParticipant);
                });
    }

    private ParticipantEntity getOrCreatePairParticipant(TournamentEntity tournament, PersonEntity person1, PersonEntity person2) {
        return participantRepository.findByTournamentId(tournament.getId()).stream()
                .filter(p -> p.getParticipantType() == ParticipantType.PAIR)
                .filter(p -> p.getMembers().contains(person1) && p.getMembers().contains(person2))
                .findFirst()
                .orElseGet(() -> {
                    ParticipantEntity newParticipant = ParticipantEntity.builder()
                            .tournament(tournament)
                            .participantSource(ParticipantSource.EXISTING_PERSON)
                            .participantType(ParticipantType.PAIR)
                            .entryStatus(EntryStatus.DIRECT_ACCEPTANCE)
                            .members(List.of(person1, person2))
                            .build();
                    return participantRepository.save(newParticipant);
                });
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

    private void accumulateCounter(Map<Integer, CategoryCounterAccumulator> countersByCategory, TournamentInscriptionPlayerView player) {
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

    private void validateTournamentOwner(TournamentEntity tournament, MemberEntity requester) {
        if (tournament.getCreatedBy() == null || tournament.getCreatedBy().getId() == null) {
            throw new IllegalStateException("El torneo no tiene creador asociado");
        }

        if (!tournament.getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("Solo el creador puede añadir jugadores manualmente al torneo");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean equalsNormalized(String left, String right) {
        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String normalizeNullable(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
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

        private TournamentInscriptionCategoryCount toResponse() {
            List<TournamentInscriptionGenderCount> genders = new ArrayList<>();
            this.totalsByGender.forEach((gender, totalPlayersByGender) ->
                    genders.add(new TournamentInscriptionGenderCount(gender, totalPlayersByGender))
            );

            return new TournamentInscriptionCategoryCount(categoryId, category, totalPlayers, genders);
        }
    }
}
