package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentMapperTest {

    private final TournamentMapper mapper = Mappers.getMapper(TournamentMapper.class);

    @Test
    void should_map_entity_to_domain() {
        UUID creatorId = UUID.randomUUID();
        TournamentEntity entity = TournamentEntity.builder()
                .id(UUID.randomUUID())
                .formalName("Open de Primavera")
                .playStartDate(LocalDate.of(2026, 5, 1))
                .playEndDate(LocalDate.of(2026, 5, 10))
                .inscriptionStartDate(LocalDate.of(2026, 4, 1))
                .inscriptionEndDate(LocalDate.of(2026, 4, 20))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .status(TournamentStatus.OPEN)
                .createdBy(MemberEntity.builder().id(creatorId).build())
                .build();

        Tournament tournament = mapper.toDomain(entity);

        assertEquals("Open de Primavera", tournament.getName());
        assertEquals(LocalDate.of(2026, 5, 1), tournament.getPlayPeriod().startDate());
        assertEquals(creatorId, tournament.getCreatedBy());
        assertEquals(TournamentStatus.OPEN, tournament.getState());
    }

    @Test
    void should_map_domain_to_entity() {
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.OPEN)
                .createdBy(creatorId)
                .eventIds(List.of())
                .build();

        TournamentEntity entity = mapper.toEntity(tournament);

        assertEquals("Open de Primavera", entity.getFormalName());
        assertEquals(LocalDate.of(2026, 5, 1), entity.getPlayStartDate());
        assertEquals(TournamentStatus.OPEN, entity.getStatus());
        assertEquals(creatorId, entity.getCreatedBy().getId());
    }
}