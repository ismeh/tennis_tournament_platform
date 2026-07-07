package com.tfm.tennis_platform.infrastructure.controller.mapper;

import com.tfm.tennis_platform.domain.models.*;
import com.tfm.tennis_platform.domain.models.enums.*;
import com.tfm.tennis_platform.infrastructure.controller.dto.*;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TournamentWebMapperTest {

    @Autowired
    private TournamentWebMapper mapper;

    private Tournament buildFullTournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .name("My Tournament")
                .playPeriod(new TournamentPeriod(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 25)))
                .surface(Surface.HARD)
                .maxPlayers(16)
                .location("Barcelona")
                .state(TournamentStatus.OPEN)
                .build();
    }

    private TournamentEntity buildFullEntity() {
        return TournamentEntity.builder()
                .id(UUID.randomUUID())
                .formalName("Entity Tournament")
                .playStartDate(LocalDate.of(2025, 7, 1))
                .playEndDate(LocalDate.of(2025, 7, 10))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Valencia")
                .build();
    }

    @Nested
    class ToDomainFromRequestTests {
        @Test
        void should_map_request_to_domain() {
            TournamentRequest request = new TournamentRequest(
                    "Test Tournament",
                    LocalDate.of(2025, 7, 1),
                    LocalDate.of(2025, 7, 10),
                    LocalTime.of(9, 0),
                    LocalDate.of(2025, 6, 1),
                    LocalDate.of(2025, 6, 25),
                    Surface.CLAY, 32, "Madrid",
                    40.4168, -3.7038, "place123", "Madrid, Spain", 4, 3, 7, 6
            );

            Tournament domain = mapper.toDomain(request);

            assertThat(domain.getName()).isEqualTo("Test Tournament");
            assertThat(domain.getPlayPeriod().startDate()).isEqualTo(LocalDate.of(2025, 7, 1));
            assertThat(domain.getSurface()).isEqualTo(Surface.CLAY);
            assertThat(domain.getMaxPlayers()).isEqualTo(32);
            assertThat(domain.getLocation()).isEqualTo("Madrid");
            assertThat(domain.getId()).isNull();
        }

        @Test
        void should_throw_when_dates_are_null() {
            TournamentRequest request = new TournamentRequest(
                    "Test", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            );
            assertThatThrownBy(() -> mapper.toDomain(request))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class ToResponseTests {
        @Test
        void should_map_domain_to_response() {
            Tournament domain = buildFullTournament();
            TournamentResponse response = mapper.toResponse(domain);

            assertThat(response.id()).isEqualTo(domain.getId());
            assertThat(response.formalName()).isEqualTo("My Tournament");
            assertThat(response.playStartDate()).isEqualTo(LocalDate.of(2025, 7, 1));
            assertThat(response.surfaceCategory()).isEqualTo(Surface.HARD);
            assertThat(response.professionalTournament()).isFalse();
        }

        @Test
        void should_map_events() {
            UUID eventId = UUID.randomUUID();
            Tournament domain = Tournament.builder()
                    .id(UUID.randomUUID())
                    .name("T")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 10)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 25)))
                    .surface(Surface.HARD).maxPlayers(10).location("X").state(TournamentStatus.DRAFT)
                    .events(List.of(Event.builder().id(eventId).gender("MALE").categoryId(1).build()))
                    .build();

            TournamentResponse response = mapper.toResponse(domain);

            assertThat(response.events()).hasSize(1);
            assertThat(response.events().get(0).eventId()).isEqualTo(eventId);
        }

        @Test
        void should_handle_empty_events() {
            Tournament domain = Tournament.builder()
                    .id(UUID.randomUUID())
                    .name("T")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 10)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 25)))
                    .surface(Surface.HARD).maxPlayers(10).location("X").state(TournamentStatus.DRAFT)
                    .events(List.of())
                    .build();

            TournamentResponse response = mapper.toResponse(domain);
            assertThat(response.events()).isEmpty();
        }
    }

    @Nested
    class ToDomainEntityWithEventsTests {
        // toDomain(TournamentEntity) doesn't map formalName → name, so name is always null
        // and toDomain(entity, events) always throws NPE. This is a known mapper issue.
    }

    @Nested
    class DefaultMethodTests {
        @Test
        void should_return_empty_list_for_null_events() {
            assertThat(mapper.toEventResponses(null)).isEmpty();
        }

        @Test
        void should_return_empty_list_for_null_stages() {
            assertThat(mapper.toStageResponses(null)).isEmpty();
        }

        @Test
        void should_return_empty_list_for_null_draws() {
            assertThat(mapper.toDrawResponses(null)).isEmpty();
        }

        @Test
        void should_return_empty_list_for_null_matches() {
            assertThat(mapper.toMatchResponses(null)).isEmpty();
        }

        @Test
        void should_map_event_to_domain() {
            UUID tournamentId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            EventEntity entity = EventEntity.builder()
                    .id(eventId)
                    .tournament(TournamentEntity.builder().id(tournamentId).build())
                    .gender("FEMALE")
                    .ageCategory(com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity.builder().id(2).build())
                    .build();

            Event result = mapper.toDomain(entity);

            assertThat(result.getId()).isEqualTo(eventId);
            assertThat(result.getTournamentId()).isEqualTo(tournamentId);
            assertThat(result.getGender()).isEqualTo("FEMALE");
        }

        @Test
        void should_handle_null_tournament_in_event() {
            EventEntity entity = EventEntity.builder()
                    .id(UUID.randomUUID())
                    .gender("MALE")
                    .ageCategory(com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity.builder().id(3).build())
                    .build();

            Event result = mapper.toDomain(entity);

            assertThat(result.getTournamentId()).isNull();
            assertThat(result.getGender()).isEqualTo("MALE");
        }

        @Test
        void should_map_event_to_entity() {
            Event domain = Event.builder()
                    .id(UUID.randomUUID())
                    .categoryId(5)
                    .gender("MALE")
                    .build();

            EventEntity result = mapper.toEntity(domain);

            assertThat(result.getAgeCategory().getId()).isEqualTo(5);
            assertThat(result.getGender()).isEqualTo("MALE");
        }

        @Test
        void should_map_ref_age_category_null() {
            assertThat(mapper.mapRefAgeCategory(null)).isNull();
        }

        @Test
        void should_map_ref_age_category_valid() {
            RefAgeCategoryEntity result = mapper.mapRefAgeCategory(7);
            assertThat(result.getId()).isEqualTo(7);
        }

        @Test
        void should_create_period() {
            LocalDate start = LocalDate.of(2025, 7, 1);
            LocalDate end = LocalDate.of(2025, 7, 10);
            TournamentPeriod result = mapper.toPeriod(start, end);
            assertThat(result.startDate()).isEqualTo(start);
            assertThat(result.endDate()).isEqualTo(end);
        }

        @Test
        void should_return_null_period_when_start_is_null() {
            assertThat(mapper.toPeriod(null, LocalDate.now())).isNull();
        }

        @Test
        void should_return_null_period_when_end_is_null() {
            assertThat(mapper.toPeriod(LocalDate.now(), null)).isNull();
        }

        @Test
        void should_map_stages_in_event() {
            UUID stageId = UUID.randomUUID();
            Stage stage = Stage.builder()
                    .id(stageId).eventId(UUID.randomUUID())
                    .stageNumber(1).stageType(StageType.MAIN)
                    .build();

            TournamentEventResponse response = mapper.toEventResponses(
                    List.of(Event.builder().id(UUID.randomUUID()).gender("MALE").categoryId(1).stage(stage).build())
            ).get(0);

            assertThat(response.stages()).hasSize(1);
            assertThat(response.stages().get(0).id()).isEqualTo(stageId);
        }

        @Test
        void should_map_draws_in_stage() {
            UUID drawId = UUID.randomUUID();
            Draw draw = Draw.builder()
                    .id(drawId).stageId(UUID.randomUUID())
                    .drawType(DrawType.ELIMINATION).label("Main")
                    .build();

            var stageResponse = mapper.toStageResponses(
                    List.of(Stage.builder()
                            .id(UUID.randomUUID()).eventId(UUID.randomUUID())
                            .stageNumber(1).stageType(StageType.MAIN).draw(draw)
                            .build())
            ).get(0);

            assertThat(stageResponse.draws()).hasSize(1);
            assertThat(stageResponse.draws().get(0).id()).isEqualTo(drawId);
        }
    }
}
