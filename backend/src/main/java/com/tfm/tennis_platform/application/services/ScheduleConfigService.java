package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleConfigService {

    private final ScheduleConfigRepository scheduleConfigRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentService tournamentService;

    public ScheduleConfig findByTournamentId(UUID tournamentId) {
        return scheduleConfigRepository.findByTournamentId(tournamentId)
                .orElse(null);
    }

    @Transactional
    public ScheduleConfig save(UUID tournamentId, List<TimeSlot> timeSlots, int matchDurationMinutes, String requesterEmail) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        tournamentService.assertTournamentAdmin(tournament, requesterEmail);

        if (matchDurationMinutes <= 0 || matchDurationMinutes > 480) {
            throw new InvalidArgumentException("La duración del partido debe estar entre 1 y 480 minutos.");
        }

        if (timeSlots == null) {
            timeSlots = new ArrayList<>();
        }

        for (TimeSlot slot : timeSlots) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw new InvalidArgumentException(
                        "La hora de fin debe ser posterior a la hora de inicio en las franjas horarias.");
            }
        }

        List<TimeSlot> sorted = timeSlots.stream()
                .sorted(Comparator.comparing(TimeSlot::startTime))
                .toList();

        for (int i = 0; i < sorted.size() - 1; i++) {
            TimeSlot current = sorted.get(i);
            TimeSlot next = sorted.get(i + 1);
            if (current.endTime().isAfter(next.startTime()) || current.endTime().equals(next.startTime())) {
                throw new InvalidArgumentException(
                        "Las franjas horarias no pueden solaparse. Solapamiento detectado entre " +
                        current.startTime() + "-" + current.endTime() + " y " +
                        next.startTime() + "-" + next.endTime() + ".");
            }
        }

        ScheduleConfig existing = scheduleConfigRepository.findByTournamentId(tournamentId).orElse(null);

        UUID configId;
        if (existing != null) {
            configId = existing.getId();
        } else {
            configId = UUID.randomUUID();
        }

        List<TimeSlot> slotsWithIds = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            TimeSlot s = sorted.get(i);
            slotsWithIds.add(new TimeSlot(s.startTime(), s.endTime()));
        }

        ScheduleConfig config = ScheduleConfig.builder()
                .id(configId)
                .tournamentId(tournamentId)
                .timeSlots(slotsWithIds)
                .matchDurationMinutes(matchDurationMinutes)
                .build();

        return scheduleConfigRepository.save(config);
    }
}
