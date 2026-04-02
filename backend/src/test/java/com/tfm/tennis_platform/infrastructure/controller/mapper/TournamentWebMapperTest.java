package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TournamentWebMapperTest {

    private final TournamentWebMapper mapper = Mappers.getMapper(TournamentWebMapper.class);

    @Test
    void should_map_request_to_domain() {
        TournamentRequest request = new TournamentRequest(
                "Open de Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central"
        );

        Tournament tournament = mapper.toDomain(request);

        assertEquals("Open de Primavera", tournament.getName());
        assertEquals(LocalDate.of(2026, 5, 1), tournament.getPlayPeriod().startDate());
        assertEquals(LocalDate.of(2026, 4, 20), tournament.getInscriptionPeriod().endDate());
        assertEquals(Surface.CLAY, tournament.getSurface());
        assertEquals(32, tournament.getMaxPlayers());
    }

    @Test
    void should_map_domain_to_response() {
        UUID organizerId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(organizerId)
                .eventIds(List.of())
                .build();

        TournamentResponse response = mapper.toResponse(tournament);

        assertEquals("Open de Primavera", response.formalName());
        assertEquals(LocalDate.of(2026, 5, 1), response.playStartDate());
        assertEquals(LocalDate.of(2026, 4, 20), response.inscriptionEndDate());
        assertEquals(Surface.CLAY, response.surfaceCategory());
        assertEquals(TournamentStatus.DRAFT, response.status());
        assertEquals(organizerId, response.providerOrganisationId());
    }
}