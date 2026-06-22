package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MemberRepository memberRepository;
    private final CourtRepository courtRepository;
    private final ScheduleConfigRepository scheduleConfigRepository;

    @Transactional
    public Tournament create(Tournament tournament, String creatorEmail, Integer courtCount) {
        Member creator = memberRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Member", creatorEmail));

        if (creator.getRole() != UserRole.ORGANIZER) {
            throw new AccessDeniedException("Only organizers can create tournaments.");
        }

        if (tournament.getStartTime() == null) {
            throw new InvalidArgumentException("La hora de inicio del torneo es obligatoria.");
        }

        Tournament tournamentToSave = Tournament.builder()
                .id(UUID.randomUUID())
                .name(tournament.getName())
                .playPeriod(new TournamentPeriod(
                        tournament.getPlayPeriod().startDate(),
                        tournament.getPlayPeriod().endDate()))
                .startTime(tournament.getStartTime())
                .inscriptionPeriod(new TournamentPeriod(
                        tournament.getInscriptionPeriod().startDate(),
                        tournament.getInscriptionPeriod().endDate()))
                .surface(tournament.getSurface())
                .maxPlayers(tournament.getMaxPlayers())
                .location(tournament.getLocation())
                .locationLatitude(tournament.getLocationLatitude())
                .locationLongitude(tournament.getLocationLongitude())
                .locationPlaceId(tournament.getLocationPlaceId())
                .locationFormattedAddress(tournament.getLocationFormattedAddress())
                .createdBy(creator)
                .build();

        Tournament savedTournament = tournamentRepository.save(tournamentToSave);
        createInitialCourts(savedTournament.getId(), courtCount);
        createDefaultScheduleConfig(savedTournament.getId());
        return savedTournament;
    }

    public Tournament create(Tournament tournament, String creatorEmail) {
        return create(tournament, creatorEmail, 0);
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public List<TournamentSummary> findSummaries() {
        return tournamentRepository.findSummaries();
    }

    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id);
    }

    public boolean isProfessionalTournament(UUID id) {
        return tournamentRepository.isProfessionalTournament(id);
    }

    @Transactional
    public Tournament updateStatus(UUID tournamentId, TournamentStatus newStatus, String requesterEmail) {
        Tournament currentTournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        assertTournamentAdmin(currentTournament, requesterEmail);

        if (newStatus == null) {
            throw new InvalidArgumentException("Selecciona un estado válido para el torneo.");
        }

        TournamentStatus currentStatus = currentTournament.getState();
        if (currentStatus == newStatus) {
            return currentTournament;
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidArgumentException("No se puede cambiar el torneo de " + currentStatus + " a " + newStatus + ".");
        }

        Tournament updatedTournament = currentTournament.toBuilder()
                .state(newStatus)
                .build();

        return tournamentRepository.save(updatedTournament);
    }

    @Transactional
    public Tournament updateGeneralInfo(UUID tournamentId, String name, TournamentPeriod playPeriod,
                                         java.time.LocalTime startTime, TournamentPeriod inscriptionPeriod,
                                         com.tfm.tennis_platform.domain.models.enums.Surface surface,
                                         Integer maxPlayers, String location, Double locationLatitude,
                                         Double locationLongitude, String locationPlaceId,
                                         String locationFormattedAddress, String requesterEmail) {
        Tournament currentTournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        assertTournamentAdmin(currentTournament, requesterEmail);

        TournamentStatus currentStatus = currentTournament.getState();
        if (currentStatus != TournamentStatus.DRAFT && currentStatus != TournamentStatus.OPEN) {
            throw new InvalidArgumentException(
                    "Solo se puede editar la información general de un torneo en borrador o con inscripciones abiertas.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new InvalidArgumentException("El nombre del torneo es obligatorio.");
        }
        if (playPeriod == null) {
            throw new InvalidArgumentException("Las fechas de juego son obligatorias.");
        }
        if (inscriptionPeriod == null) {
            throw new InvalidArgumentException("Las fechas de inscripción son obligatorias.");
        }
        if (surface == null) {
            throw new InvalidArgumentException("La superficie del torneo es obligatoria.");
        }
        if (maxPlayers == null || maxPlayers <= 0) {
            throw new InvalidArgumentException("El número máximo de jugadores debe ser mayor que cero.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new InvalidArgumentException("La ubicación del torneo es obligatoria.");
        }
        if (startTime == null) {
            throw new InvalidArgumentException("La hora de inicio del torneo es obligatoria.");
        }

        Tournament updatedTournament = currentTournament.toBuilder()
                .name(name.trim())
                .playPeriod(playPeriod)
                .startTime(startTime)
                .inscriptionPeriod(inscriptionPeriod)
                .surface(surface)
                .maxPlayers(maxPlayers)
                .location(location.trim())
                .locationLatitude(locationLatitude)
                .locationLongitude(locationLongitude)
                .locationPlaceId(locationPlaceId)
                .locationFormattedAddress(locationFormattedAddress)
                .build();

        return tournamentRepository.save(updatedTournament);
    }

    public void assertTournamentAdmin(UUID tournamentId, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        assertTournamentAdmin(tournament, requesterEmail);
    }

    public void assertTournamentAdmin(Tournament tournament, String requesterEmail) {
        if (tournament == null || tournament.getCreatedBy() == null || requesterEmail == null) {
            throw new AccessDeniedException("Only the tournament administrator can perform this action.");
        }

        Member requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new AccessDeniedException("Only the tournament administrator can perform this action."));
        UUID tournamentAdminId = tournament.getCreatedBy().getId();
        if (tournamentAdminId != null && requester.getId() != null && tournamentAdminId.equals(requester.getId())) {
            return;
        }

        String tournamentAdminEmail = tournament.getCreatedBy().getEmail();
        if (tournamentAdminEmail == null || !requesterEmail.equalsIgnoreCase(tournamentAdminEmail)) {
            throw new AccessDeniedException("Only the tournament administrator can perform this action.");
        }
    }

    private void createInitialCourts(UUID tournamentId, Integer courtCount) {
        if (courtCount == null || courtCount == 0) {
            return;
        }
        if (courtCount < 0) {
            throw new InvalidArgumentException("El número de pistas no puede ser negativo.");
        }

        for (int courtNumber = 1; courtNumber <= courtCount; courtNumber++) {
            String courtName = "Pista " + courtNumber;
            if (courtRepository.existsByTournamentIdAndName(tournamentId, courtName)) {
                continue;
            }

            courtRepository.save(Court.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournamentId)
                    .name(courtName)
                    .active(true)
                    .build());
        }
    }

    private void createDefaultScheduleConfig(UUID tournamentId) {
        ScheduleConfig config = ScheduleConfig.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .matchDurationMinutes(60)
                .timeSlots(List.of(
                        new TimeSlot(LocalTime.of(8, 0), LocalTime.of(13, 0)),
                        new TimeSlot(LocalTime.of(16, 0), LocalTime.of(20, 0))
                ))
                .build();
        scheduleConfigRepository.save(config);
    }
}
