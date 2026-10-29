package com.tfm.tennis_platform.infrastructure.integration;

import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.models.enums.ParticipantType;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionResult;
import com.tfm.tennis_platform.application.services.InscriptionService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration-test")
@DisplayName("RNF-09 - Concurrencia de inscripciones (50 usuarios simultaneos)")
class InscriptionConcurrencyIT {

    @Autowired
    private InscriptionService inscriptionService;

    @Autowired
    private JpaTournamentRepository tournamentRepository;

    @Autowired
    private JpaEventRepository eventRepository;

    @Autowired
    private JpaMemberRepository memberRepository;

    @Autowired
    private JpaPersonRepository personRepository;

    @Autowired
    private JpaParticipantRepository participantRepository;

    @Autowired
    private JpaInscriptionRepository inscriptionRepository;

    private TournamentEntity tournament;
    private EventEntity event;
    private MemberEntity ownerMember;
    private PersonEntity ownerPerson;
    private final List<MemberEntity> playerMembers = new ArrayList<>();
    private final List<PersonEntity> playerPersons = new ArrayList<>();

    @BeforeEach
    void setUp() {
        inscriptionRepository.deleteAll();
        participantRepository.deleteAll();
        eventRepository.deleteAll();
        tournamentRepository.deleteAll();
        memberRepository.deleteAll();
        personRepository.deleteAll();

        ownerPerson = personRepository.save(PersonEntity.builder()
                .firstName("Owner")
                .lastName("Torneo")
                .gender("H")
                .birthDate(LocalDate.of(1990, Month.JANUARY, 1))
                .nationality("ESP")
                .build());

        ownerMember = memberRepository.save(MemberEntity.builder()
                .email("owner@concurrency.test")
                .passwordHash("hash")
                .emailVerified(true)
                .personId(ownerPerson.getId())
                .build());

        tournament = tournamentRepository.save(TournamentEntity.builder()
                .id(UUID.randomUUID())
                .formalName("Torneo Concurrencia")
                .playStartDate(LocalDate.now().plusDays(1))
                .playEndDate(LocalDate.now().plusDays(2))
                .inscriptionStartDate(LocalDate.now().minusDays(1))
                .inscriptionEndDate(LocalDate.now().plusDays(1))
                .surface(com.tfm.tennis_platform.domain.models.enums.Surface.CLAY)
                .maxPlayers(100)
                .location("Madrid")
                .status(TournamentStatus.OPEN)
                .createdBy(ownerMember)
                .build());

        event = eventRepository.save(EventEntity.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .name("Individual Masculino")
                .gender("H")
                .eventType("SINGLE")
                .build());
    }

    @Test
    @DisplayName("50 usuarios concurrentes inscribiendose para el mismo evento")
    void shouldHandle50ConcurrentInscriptionsWithoutDataLoss() throws Exception {
        int totalPlayers = 50;
        CountDownLatch readyLatch = new CountDownLatch(totalPlayers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<EventInscriptionResult> successes = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<Exception> failures = new CopyOnWriteArrayList<>();
        AtomicInteger duplicateCount = new AtomicInteger(0);

        for (int i = 0; i < totalPlayers; i++) {
            int index = i;
            playerPersons.add(personRepository.save(PersonEntity.builder()
                    .firstName("Player" + index)
                    .lastName("Test" + index)
                    .gender("H")
                    .birthDate(LocalDate.of(1995, Month.JANUARY, 1))
                    .nationality("ESP")
                    .tennisId("T" + String.format("%04d", index))
                    .build()));

            playerMembers.add(memberRepository.save(MemberEntity.builder()
                    .email("player" + index + "@concurrency.test")
                    .passwordHash("hash")
                    .emailVerified(true)
                    .personId(playerPersons.get(index).getId())
                    .build()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalPlayers);

        for (int i = 0; i < totalPlayers; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    EventInscriptionCommand command = new EventInscriptionCommand(null, null);
                    EventInscriptionResult result = inscriptionService.register(
                            tournament.getId(),
                            event.getId(),
                            command,
                            playerMembers.get(index).getEmail()
                    );
                    successes.add(result);
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("ya esta inscrito")) {
                        duplicateCount.incrementAndGet();
                    } else {
                        failures.add(e);
                    }
                } catch (Exception e) {
                    failures.add(e);
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long inscriptionsInDb = inscriptionRepository.findByEvent_Id(event.getId()).size();

        assertThat(inscriptionsInDb)
                .as("Todas las inscripciones deben persistirse sin perdida de datos")
                .isEqualTo(totalPlayers);

        assertThat(failures)
                .as("No deben haber errores inesperados: %s", failures)
                .isEmpty();

        assertThat(successes.size() + duplicateCount.get())
                .as("El total de exitos + duplicados debe ser igual al numero de usuarios")
                .isEqualTo(totalPlayers);
    }

    @Test
    @DisplayName("50 usuarios concurrentes inscribiendose para diferentes eventos")
    void shouldHandle50ConcurrentInscriptionsAcrossDifferentEvents() throws Exception {
        int totalPlayers = 50;
        List<EventEntity> events = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            events.add(eventRepository.save(EventEntity.builder()
                    .id(UUID.randomUUID())
                    .tournament(tournament)
                    .name("Evento" + i)
                    .gender(i % 2 == 0 ? "H" : "M")
                    .eventType("SINGLE")
                    .build()));
        }

        CountDownLatch readyLatch = new CountDownLatch(totalPlayers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<EventInscriptionResult> successes = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<Exception> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < totalPlayers; i++) {
            int index = i;
            playerPersons.add(personRepository.save(PersonEntity.builder()
                    .firstName("MultiPlayer" + index)
                    .lastName("Test" + index)
                    .gender("H")
                    .birthDate(LocalDate.of(1995, Month.JANUARY, 1))
                    .nationality("ESP")
                    .tennisId("M" + String.format("%04d", index))
                    .build()));

            playerMembers.add(memberRepository.save(MemberEntity.builder()
                    .email("multiplayer" + index + "@concurrency.test")
                    .passwordHash("hash")
                    .emailVerified(true)
                    .personId(playerPersons.get(index).getId())
                    .build()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalPlayers);

        for (int i = 0; i < totalPlayers; i++) {
            final int index = i;
            final EventEntity targetEvent = events.get(index % events.size());
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    EventInscriptionCommand command = new EventInscriptionCommand(null, null);
                    EventInscriptionResult result = inscriptionService.register(
                            tournament.getId(),
                            targetEvent.getId(),
                            command,
                            playerMembers.get(index).getEmail()
                    );
                    successes.add(result);
                } catch (Exception e) {
                    failures.add(e);
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long totalInscriptions = 0;
        for (EventEntity e : events) {
            totalInscriptions += inscriptionRepository.findByEvent_Id(e.getId()).size();
        }

        assertThat(totalInscriptions)
                .as("Todas las inscripciones deben persistirse across eventos")
                .isEqualTo(totalPlayers);

        assertThat(failures)
                .as("No deben haber errores: %s", failures)
                .isEmpty();
    }
}
