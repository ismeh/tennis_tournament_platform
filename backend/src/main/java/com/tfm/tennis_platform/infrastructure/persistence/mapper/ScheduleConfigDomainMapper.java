package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ScheduleConfigEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TimeSlotEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScheduleConfigDomainMapper {

    private final JpaTournamentRepository tournamentRepository;

    public ScheduleConfig toDomain(ScheduleConfigEntity entity) {
        if (entity == null) {
            return null;
        }

        List<TimeSlot> timeSlots = entity.getTimeSlots() != null
                ? entity.getTimeSlots().stream()
                    .sorted(Comparator.comparing(TimeSlotEntity::getSlotOrder))
                    .map(this::toDomain)
                    .collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>();

        return ScheduleConfig.builder()
                .id(entity.getId())
                .tournamentId(entity.getTournament() != null ? entity.getTournament().getId() : null)
                .timeSlots(timeSlots)
                .matchDurationMinutes(entity.getMatchDurationMinutes())
                .build();
    }

    public ScheduleConfigEntity toEntity(ScheduleConfig domain) {
        if (domain == null) {
            return null;
        }

        TournamentEntity tournamentRef = domain.getTournamentId() != null
                ? tournamentRepository.getReferenceById(domain.getTournamentId())
                : null;

        List<TimeSlotEntity> timeSlotEntities = domain.getTimeSlots() != null
                ? new ArrayList<>()
                : new ArrayList<>();

        ScheduleConfigEntity entity = ScheduleConfigEntity.builder()
                .id(domain.getId())
                .tournament(tournamentRef)
                .matchDurationMinutes(domain.getMatchDurationMinutes())
                .timeSlots(timeSlotEntities)
                .build();

        if (domain.getTimeSlots() != null) {
            int order = 0;
            for (TimeSlot slot : domain.getTimeSlots()) {
                TimeSlotEntity slotEntity = TimeSlotEntity.builder()
                        .id(UUID.randomUUID())
                        .scheduleConfig(entity)
                        .startTime(slot.startTime())
                        .endTime(slot.endTime())
                        .slotOrder(order++)
                        .build();
                entity.getTimeSlots().add(slotEntity);
            }
        }

        return entity;
    }

    private TimeSlot toDomain(TimeSlotEntity entity) {
        return new TimeSlot(entity.getStartTime(), entity.getEndTime());
    }
}
