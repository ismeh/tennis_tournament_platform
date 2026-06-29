package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.*;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TournamentEntityMapperTest {

    @Autowired
    private TournamentEntityMapper mapper;

    private static final UUID TOURNAMENT_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID STAGE_ID = UUID.randomUUID();
    private static final UUID DRAW_ID = UUID.randomUUID();
    private static final UUID MATCH_ID = UUID.randomUUID();
    private static final UUID CREATED_BY_ID = UUID.randomUUID();
    private static final UUID FIRST_INSCRIPTION_ID = UUID.randomUUID();
    private static final UUID SECOND_INSCRIPTION_ID = UUID.randomUUID();
    private static final UUID WINNER_INSCRIPTION_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();

    @Nested
    class ToDomainTests {

        @Test
        void should_map_entity_to_domain_with_full_hierarchy() {
            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .label("Main Draw")
                    .groupIndex(0)
                    .build();

            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .draw(drawEntity)
                    .roundNumber(1)
                    .bracketPosition(0)
                    .scheduledAt(LocalDateTime.of(2026, 7, 15, 10, 0))
                    .scheduleTimeType(ScheduleTimeType.EXACT)
                    .result("6-3 7-5")
                    .court("Court 1")
                    .firstInscription(InscriptionEntity.builder().id(FIRST_INSCRIPTION_ID).build())
                    .secondInscription(InscriptionEntity.builder().id(SECOND_INSCRIPTION_ID).build())
                    .winner(InscriptionEntity.builder().id(WINNER_INSCRIPTION_ID).build())
                    .build();

            drawEntity.setMatches(List.of(matchEntity));

            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .strategyName("single_elimination")
                    .description("Main stage")
                    .draws(List.of(drawEntity))
                    .build();

            EventEntity eventEntity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(5).build())
                    .stages(List.of(stageEntity))
                    .build();

            MemberEntity createdByEntity = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Test Tournament")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .startTime(LocalTime.of(9, 0))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(64)
                    .location("Madrid")
                    .status(TournamentStatus.OPEN)
                    .createdBy(createdByEntity)
                    .events(List.of(eventEntity))
                    .build();

            Tournament domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(TOURNAMENT_ID);
            assertThat(domain.getName()).isEqualTo("Test Tournament");
            assertThat(domain.getPlayPeriod()).isNotNull();
            assertThat(domain.getPlayPeriod().startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(domain.getPlayPeriod().endDate()).isEqualTo(LocalDate.of(2026, 7, 10));
            assertThat(domain.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(domain.getInscriptionPeriod()).isNotNull();
            assertThat(domain.getInscriptionPeriod().startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(domain.getInscriptionPeriod().endDate()).isEqualTo(LocalDate.of(2026, 6, 25));
            assertThat(domain.getSurface()).isEqualTo(Surface.HARD);
            assertThat(domain.getMaxPlayers()).isEqualTo(64);
            assertThat(domain.getLocation()).isEqualTo("Madrid");
            assertThat(domain.getState()).isEqualTo(TournamentStatus.OPEN);
            assertThat(domain.getCreatedBy()).isNotNull();
            assertThat(domain.getCreatedBy().getId()).isEqualTo(CREATED_BY_ID);

            assertThat(domain.getEvents()).hasSize(1);
            Event event = domain.getEvents().getFirst();
            assertThat(event.getId()).isEqualTo(EVENT_ID);
            assertThat(event.getGender()).isEqualTo("MALE");
            assertThat(event.getCategoryId()).isEqualTo(5);

            assertThat(event.getStages()).hasSize(1);
            Stage stage = event.getStages().getFirst();
            assertThat(stage.getId()).isEqualTo(STAGE_ID);
            assertThat(stage.getStageNumber()).isEqualTo(1);
            assertThat(stage.getStageType()).isEqualTo(StageType.MAIN);
            assertThat(stage.getStrategyName()).isEqualTo("single_elimination");
            assertThat(stage.getDescription()).isEqualTo("Main stage");

            assertThat(stage.getDraws()).hasSize(1);
            Draw draw = stage.getDraws().getFirst();
            assertThat(draw.getId()).isEqualTo(DRAW_ID);
            assertThat(draw.getDrawType()).isEqualTo(DrawType.ELIMINATION);
            assertThat(draw.getLabel()).isEqualTo("Main Draw");
            assertThat(draw.getDrawName()).isEqualTo("Main Draw");
            assertThat(draw.getGroupIndex()).isEqualTo(0);

            assertThat(draw.getMatches()).hasSize(1);
            Match match = draw.getMatches().getFirst();
            assertThat(match.getId()).isEqualTo(MATCH_ID);
            assertThat(match.getDrawId()).isEqualTo(DRAW_ID);
            assertThat(match.getRoundNumber()).isEqualTo(1);
            assertThat(match.getBracketPosition()).isEqualTo(0);
            assertThat(match.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 10, 0));
            assertThat(match.getScheduleTimeType()).isEqualTo(ScheduleTimeType.EXACT);
            assertThat(match.getResult()).isEqualTo("6-3 7-5");
            assertThat(match.getCourt()).isEqualTo("Court 1");
            assertThat(match.getFirstInscription()).isNotNull();
            assertThat(match.getFirstInscription().getId()).isEqualTo(FIRST_INSCRIPTION_ID);
            assertThat(match.getSecondInscription()).isNotNull();
            assertThat(match.getSecondInscription().getId()).isEqualTo(SECOND_INSCRIPTION_ID);
            assertThat(match.getWinner()).isNotNull();
            assertThat(match.getWinner().getId()).isEqualTo(WINNER_INSCRIPTION_ID);
        }

        @Test
        void should_handle_null_events() {
            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Empty Tournament")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.CLAY)
                    .maxPlayers(32)
                    .location("Barcelona")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(null)
                    .build();

            Tournament domain = mapper.toDomain(entity);

            assertThat(domain.getEvents()).isNotNull().isEmpty();
        }

        @Test
        void should_handle_empty_events() {
            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Empty Events Tournament")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.GRASS)
                    .maxPlayers(16)
                    .location("London")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(new ArrayList<>())
                    .build();

            Tournament domain = mapper.toDomain(entity);

            assertThat(domain.getEvents()).isNotNull().isEmpty();
        }

        @Test
        void should_handle_null_created_by() {
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("No Creator Tournament")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Seville")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(null)
                    .events(new ArrayList<>())
                    .build();

            Tournament domain = mapper.toDomain(entity);

            assertThat(domain.getCreatedBy()).isNull();
        }

        @Test
        void should_throw_when_event_entity_has_null_age_category() {
            EventEntity eventEntity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("FEMALE")
                    .ageCategory(null)
                    .stages(new ArrayList<>())
                    .build();

            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Test")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Valencia")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(List.of(eventEntity))
                    .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_map_match_with_null_court_resource_fallback_to_court_string() {
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .court("Court 3")
                    .roundNumber(1)
                    .build();

            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .matches(List.of(matchEntity))
                    .build();

            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Test")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Mallorca")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(new ArrayList<>())
                    .build();

            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .draws(List.of(drawEntity))
                    .build();

            EventEntity eventEntity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                    .stages(List.of(stageEntity))
                    .build();

            entity.setEvents(List.of(eventEntity));

            Tournament domain = mapper.toDomain(entity);

            Match match = domain.getEvents().getFirst().getStages().getFirst()
                    .getDraws().getFirst().getMatches().getFirst();
            assertThat(match.getCourt()).isEqualTo("Court 3");
            assertThat(match.getCourtId()).isNull();
        }

        @Test
        void should_map_match_with_court_resource() {
            CourtEntity court = CourtEntity.builder().id(COURT_ID).name("Center Court").build();
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .courtResource(court)
                    .roundNumber(2)
                    .build();

            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .matches(List.of(matchEntity))
                    .build();

            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .draws(List.of(drawEntity))
                    .build();

            EventEntity eventEntity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                    .stages(List.of(stageEntity))
                    .build();

            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Court Ref Test")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Madrid")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(List.of(eventEntity))
                    .build();

            Tournament domain = mapper.toDomain(entity);

            Match match = domain.getEvents().getFirst().getStages().getFirst()
                    .getDraws().getFirst().getMatches().getFirst();
            assertThat(match.getCourtId()).isEqualTo(COURT_ID);
            assertThat(match.getCourt()).isEqualTo("Center Court");
        }

        @Test
        void should_map_match_with_next_match_reference() {
            MatchEntity nextMatchEntity = MatchEntity.builder()
                    .id(UUID.randomUUID())
                    .roundNumber(2)
                    .bracketPosition(0)
                    .build();

            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .roundNumber(1)
                    .bracketPosition(0)
                    .nextMatch(nextMatchEntity)
                    .build();

            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .matches(List.of(matchEntity, nextMatchEntity))
                    .build();

            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .draws(List.of(drawEntity))
                    .build();

            EventEntity eventEntity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                    .stages(List.of(stageEntity))
                    .build();

            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Next Match Test")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Madrid")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(List.of(eventEntity))
                    .build();

            Tournament domain = mapper.toDomain(entity);

            Match match = domain.getEvents().getFirst().getStages().getFirst()
                    .getDraws().getFirst().getMatches().stream()
                    .filter(m -> m.getId().equals(MATCH_ID))
                    .findFirst().orElseThrow();
            assertThat(match.getNextMatch()).isNotNull();
            assertThat(match.getNextMatch().getRoundNumber()).isEqualTo(2);
        }

        @Test
        void should_map_match_with_loser_next_match() {
            MatchEntity loserNextMatch = MatchEntity.builder()
                    .id(UUID.randomUUID())
                    .roundNumber(2)
                    .bracketPosition(1)
                    .build();

            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .roundNumber(1)
                    .bracketPosition(0)
                    .loserNextMatch(loserNextMatch)
                    .build();

            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .matches(List.of(matchEntity, loserNextMatch))
                    .build();

            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .draws(List.of(drawEntity))
                    .build();

            EventEntity eventEntity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(1).build())
                    .stages(List.of(stageEntity))
                    .build();

            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity entity = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Loser Next Match Test")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Madrid")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(List.of(eventEntity))
                    .build();

            Tournament domain = mapper.toDomain(entity);

            Match match = domain.getEvents().getFirst().getStages().getFirst()
                    .getDraws().getFirst().getMatches().stream()
                    .filter(m -> m.getId().equals(MATCH_ID))
                    .findFirst().orElseThrow();
            assertThat(match.getLoserNextMatch()).isNotNull();
            assertThat(match.getLoserNextMatch().getBracketPosition()).isEqualTo(1);
        }

        @Test
        void should_map_stage_with_null_event_reference() {
            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .event(null)
                    .order(1)
                    .stageType("MAIN")
                    .build();

            Stage stage = mapper.toDomain(stageEntity);

            assertThat(stage.getEventId()).isNull();
        }

        @Test
        void should_throw_when_stage_entity_has_null_stage_type() {
            StageEntity stageEntity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType(null)
                    .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.toDomain(stageEntity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_map_draw_with_null_stage_reference() {
            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .stage(null)
                    .drawType("ROUND_ROBIN")
                    .build();

            Draw draw = mapper.toDomain(drawEntity);

            assertThat(draw.getStageId()).isNull();
        }

        @Test
        void should_throw_when_draw_entity_has_null_draw_type() {
            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType(null)
                    .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.toDomain(drawEntity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_map_draw_with_null_label() {
            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .label(null)
                    .build();

            Draw draw = mapper.toDomain(drawEntity);

            assertThat(draw.getLabel()).isNull();
            assertThat(draw.getDrawName()).isNull();
        }

        @Test
        void should_map_draw_with_null_matches() {
            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .matches(null)
                    .build();

            Draw draw = mapper.toDomain(drawEntity);

            assertThat(draw.getMatches()).isNotNull().isEmpty();
        }

        @Test
        void should_map_draw_with_null_group_index() {
            DrawEntity drawEntity = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .groupIndex(null)
                    .build();

            Draw draw = mapper.toDomain(drawEntity);

            assertThat(draw.getGroupIndex()).isNull();
        }

        @Test
        void should_map_match_with_null_inscription_references() {
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .firstInscription(null)
                    .secondInscription(null)
                    .winner(null)
                    .roundNumber(1)
                    .build();

            Match match = mapper.toDomain(matchEntity);

            assertThat(match.getFirstInscription()).isNull();
            assertThat(match.getSecondInscription()).isNull();
            assertThat(match.getWinner()).isNull();
        }

        @Test
        void should_map_match_with_null_draw_reference() {
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .draw(null)
                    .roundNumber(1)
                    .build();

            Match match = mapper.toDomain(matchEntity);

            assertThat(match.getDrawId()).isNull();
        }

        @Test
        void should_map_match_with_null_next_match_and_loser_next_match() {
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .nextMatch(null)
                    .loserNextMatch(null)
                    .roundNumber(1)
                    .build();

            Match match = mapper.toDomain(matchEntity);

            assertThat(match.getNextMatch()).isNull();
            assertThat(match.getLoserNextMatch()).isNull();
        }

        @Test
        void should_map_match_with_null_scheduled_at_and_schedule_time_type() {
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .scheduledAt(null)
                    .scheduleTimeType(null)
                    .roundNumber(1)
                    .build();

            Match match = mapper.toDomain(matchEntity);

            assertThat(match.getScheduledAt()).isNull();
            assertThat(match.getScheduleTimeType()).isNull();
        }

        @Test
        void should_map_match_with_null_result() {
            MatchEntity matchEntity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .result(null)
                    .roundNumber(1)
                    .build();

            Match match = mapper.toDomain(matchEntity);

            assertThat(match.getResult()).isNull();
        }
    }

    @Nested
    class ToEntityTests {

        @Test
        void should_map_domain_to_entity() {
            TournamentPeriod playPeriod = new TournamentPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15));
            TournamentPeriod inscriptionPeriod = new TournamentPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 25));
            Tournament domain = Tournament.builder()
                    .id(TOURNAMENT_ID)
                    .name("Domain Tournament")
                    .playPeriod(playPeriod)
                    .startTime(LocalTime.of(10, 30))
                    .inscriptionPeriod(inscriptionPeriod)
                    .surface(Surface.CLAY)
                    .maxPlayers(128)
                    .location("Rome")
                    .state(TournamentStatus.IN_PROGRESS)
                    .events(new ArrayList<>())
                    .build();

            TournamentEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(TOURNAMENT_ID);
            assertThat(entity.getFormalName()).isEqualTo("Domain Tournament");
            assertThat(entity.getPlayStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(entity.getPlayEndDate()).isEqualTo(LocalDate.of(2026, 8, 15));
            assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(10, 30));
            assertThat(entity.getInscriptionStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(entity.getInscriptionEndDate()).isEqualTo(LocalDate.of(2026, 7, 25));
            assertThat(entity.getSurface()).isEqualTo(Surface.CLAY);
            assertThat(entity.getMaxPlayers()).isEqualTo(128);
            assertThat(entity.getLocation()).isEqualTo("Rome");
            assertThat(entity.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
        }

        @Test
        void should_map_domain_with_events_to_entity() {
            Stage stage = Stage.builder()
                    .id(STAGE_ID)
                    .stageNumber(1)
                    .stageType(StageType.MAIN)
                    .strategyName("single_elimination")
                    .description("Main stage")
                    .draws(new ArrayList<>())
                    .build();

            Event event = Event.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .categoryId(3)
                    .stages(List.of(stage))
                    .build();

            TournamentPeriod playPeriod = new TournamentPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15));
            TournamentPeriod inscriptionPeriod = new TournamentPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 25));
            Tournament domain = Tournament.builder()
                    .id(TOURNAMENT_ID)
                    .name("Event Mapping Tournament")
                    .playPeriod(playPeriod)
                    .inscriptionPeriod(inscriptionPeriod)
                    .surface(Surface.HARD)
                    .maxPlayers(64)
                    .location("Paris")
                    .state(TournamentStatus.DRAFT)
                    .events(List.of(event))
                    .build();

            TournamentEntity entity = mapper.toEntity(domain);

            assertThat(entity.getEvents()).hasSize(1);
            EventEntity eventEntity = entity.getEvents().getFirst();
            assertThat(eventEntity.getId()).isEqualTo(EVENT_ID);
            assertThat(eventEntity.getGender()).isEqualTo("MALE");
            assertThat(eventEntity.getAgeCategory()).isNotNull();
            assertThat(eventEntity.getAgeCategory().getId()).isEqualTo(3);
            assertThat(eventEntity.getStages()).hasSize(1);
        }

        @Test
        void should_map_event_domain_to_entity() {
            Event domain = Event.builder()
                    .id(EVENT_ID)
                    .gender("FEMALE")
                    .categoryId(7)
                    .stages(new ArrayList<>())
                    .build();

            EventEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(EVENT_ID);
            assertThat(entity.getGender()).isEqualTo("FEMALE");
            assertThat(entity.getAgeCategory()).isNotNull();
            assertThat(entity.getAgeCategory().getId()).isEqualTo(7);
        }

        @Test
        void should_map_stage_domain_to_entity() {
            Stage domain = Stage.builder()
                    .id(STAGE_ID)
                    .stageNumber(2)
                    .stageType(StageType.QUALIFYING)
                    .strategyName("standard")
                    .description("Qualifying round")
                    .draws(new ArrayList<>())
                    .build();

            StageEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(STAGE_ID);
            assertThat(entity.getOrder()).isEqualTo(2);
            assertThat(entity.getStageType()).isEqualTo("QUALIFYING");
            assertThat(entity.getStrategyName()).isEqualTo("standard");
            assertThat(entity.getDescription()).isEqualTo("Qualifying round");
        }

        @Test
        void should_throw_when_building_stage_with_null_stage_type() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    Stage.builder()
                            .id(STAGE_ID)
                            .stageNumber(1)
                            .stageType(null)
                            .draws(new ArrayList<>())
                            .build()
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_map_draw_domain_to_entity() {
            Draw domain = Draw.builder()
                    .id(DRAW_ID)
                    .drawType(DrawType.ROUND_ROBIN)
                    .label("Group A")
                    .drawName("Group A Name")
                    .build();

            DrawEntity entity = mapper.toEntity(domain);

            assertThat(entity.getId()).isEqualTo(DRAW_ID);
            assertThat(entity.getDrawType()).isEqualTo("ROUND_ROBIN");
            assertThat(entity.getLabel()).isEqualTo("Group A");
        }

        @Test
        void should_map_draw_fallback_to_draw_name_when_label_is_null() {
            Draw domain = Draw.builder()
                    .id(DRAW_ID)
                    .drawType(DrawType.ELIMINATION)
                    .label(null)
                    .drawName("Fallback Name")
                    .build();

            DrawEntity entity = mapper.toEntity(domain);

            assertThat(entity.getLabel()).isEqualTo("Fallback Name");
        }

        @Test
        void should_throw_when_building_draw_with_null_draw_type() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    Draw.builder()
                            .id(DRAW_ID)
                            .drawType(null)
                            .label("Something")
                            .build()
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_building_event_with_null_category_id() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    Event.builder()
                            .id(EVENT_ID)
                            .gender("MIXED")
                            .categoryId(null)
                            .stages(new ArrayList<>())
                            .build()
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class UpdateEntityFromDomainTests {

        @Test
        void should_update_entity_from_domain() {
            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity existing = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Old Name")
                    .playStartDate(LocalDate.of(2026, 1, 1))
                    .playEndDate(LocalDate.of(2026, 1, 10))
                    .inscriptionStartDate(LocalDate.of(2025, 12, 1))
                    .inscriptionEndDate(LocalDate.of(2025, 12, 25))
                    .surface(Surface.GRASS)
                    .maxPlayers(16)
                    .location("Old Location")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(new ArrayList<>())
                    .build();

            Tournament domain = Tournament.builder()
                    .id(UUID.randomUUID())
                    .name("Updated Name")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 25)))
                    .surface(Surface.CLAY)
                    .maxPlayers(128)
                    .location("New Location")
                    .state(TournamentStatus.OPEN)
                    .events(new ArrayList<>())
                    .build();

            mapper.updateEntityFromDomain(domain, existing);

            assertThat(existing.getId()).isEqualTo(TOURNAMENT_ID);
            assertThat(existing.getFormalName()).isEqualTo("Old Name");
            assertThat(existing.getPlayStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(existing.getPlayEndDate()).isEqualTo(LocalDate.of(2026, 1, 10));
            assertThat(existing.getInscriptionStartDate()).isEqualTo(LocalDate.of(2025, 12, 1));
            assertThat(existing.getInscriptionEndDate()).isEqualTo(LocalDate.of(2025, 12, 25));
            assertThat(existing.getStatus()).isEqualTo(TournamentStatus.OPEN);
            assertThat(existing.getStartTime()).isNull();
            assertThat(existing.getSurface()).isEqualTo(Surface.CLAY);
            assertThat(existing.getMaxPlayers()).isEqualTo(128);
            assertThat(existing.getLocation()).isEqualTo("New Location");
        }

        @Test
        void should_preserve_id_and_created_by_and_events_on_update() {
            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            EventEntity existingEvent = EventEntity.builder()
                    .id(UUID.randomUUID())
                    .gender("MALE")
                    .build();
            TournamentEntity existing = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Old")
                    .playStartDate(LocalDate.of(2026, 1, 1))
                    .playEndDate(LocalDate.of(2026, 1, 10))
                    .inscriptionStartDate(LocalDate.of(2025, 12, 1))
                    .inscriptionEndDate(LocalDate.of(2025, 12, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Old")
                    .status(TournamentStatus.DRAFT)
                    .createdBy(createdBy)
                    .events(List.of(existingEvent))
                    .build();

            Tournament domain = Tournament.builder()
                    .id(UUID.randomUUID())
                    .name("New")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 25)))
                    .surface(Surface.CLAY)
                    .maxPlayers(64)
                    .location("New")
                    .state(TournamentStatus.IN_PROGRESS)
                    .events(new ArrayList<>())
                    .build();

            mapper.updateEntityFromDomain(domain, existing);

            assertThat(existing.getId()).isEqualTo(TOURNAMENT_ID);
            assertThat(existing.getCreatedBy()).isSameAs(createdBy);
            assertThat(existing.getEvents()).hasSize(1);
            assertThat(existing.getEvents().getFirst()).isSameAs(existingEvent);
        }
    }

    @Nested
    class ListNullSafetyTests {

        @Test
        void toDomainEvents_should_return_empty_when_null() {
            assertThat(mapper.toDomainEvents(null)).isNotNull().isEmpty();
        }

        @Test
        void toEntityEvents_should_return_empty_when_null() {
            assertThat(mapper.toEntityEvents(null)).isNotNull().isEmpty();
        }

        @Test
        void toDomainStages_should_return_empty_when_null() {
            assertThat(mapper.toDomainStages(null)).isNotNull().isEmpty();
        }

        @Test
        void toEntityStages_should_return_empty_when_null() {
            assertThat(mapper.toEntityStages(null)).isNotNull().isEmpty();
        }

        @Test
        void toDomainDraws_should_return_empty_when_null() {
            assertThat(mapper.toDomainDraws(null)).isNotNull().isEmpty();
        }

        @Test
        void toEntityDraws_should_return_empty_when_null() {
            assertThat(mapper.toEntityDraws(null)).isNotNull().isEmpty();
        }

        @Test
        void toDomainMatches_should_return_empty_when_null() {
            assertThat(mapper.toDomainMatches(null)).isNotNull().isEmpty();
        }

        @Test
        void toDomainEvents_should_map_list() {
            EventEntity e1 = EventEntity.builder().id(UUID.randomUUID()).gender("MALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(1).build()).stages(new ArrayList<>()).build();
            EventEntity e2 = EventEntity.builder().id(UUID.randomUUID()).gender("FEMALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(2).build()).stages(new ArrayList<>()).build();

            List<Event> result = mapper.toDomainEvents(List.of(e1, e2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getGender()).isEqualTo("MALE");
            assertThat(result.get(1).getGender()).isEqualTo("FEMALE");
        }

        @Test
        void toEntityEvents_should_map_list() {
            Event e1 = Event.builder().id(UUID.randomUUID()).gender("MALE").categoryId(1).stages(new ArrayList<>()).build();
            Event e2 = Event.builder().id(UUID.randomUUID()).gender("FEMALE").categoryId(2).stages(new ArrayList<>()).build();

            List<EventEntity> result = mapper.toEntityEvents(List.of(e1, e2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getGender()).isEqualTo("MALE");
            assertThat(result.get(1).getGender()).isEqualTo("FEMALE");
        }

        @Test
        void toDomainStages_should_map_list() {
            StageEntity s1 = StageEntity.builder().id(UUID.randomUUID()).order(1).stageType("MAIN").build();
            StageEntity s2 = StageEntity.builder().id(UUID.randomUUID()).order(2).stageType("QUALIFYING").build();

            List<Stage> result = mapper.toDomainStages(List.of(s1, s2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getStageNumber()).isEqualTo(1);
            assertThat(result.get(1).getStageNumber()).isEqualTo(2);
        }

        @Test
        void toEntityStages_should_map_list() {
            Stage s1 = Stage.builder().id(UUID.randomUUID()).stageNumber(1).stageType(StageType.MAIN).draws(new ArrayList<>()).build();
            Stage s2 = Stage.builder().id(UUID.randomUUID()).stageNumber(2).stageType(StageType.QUALIFYING).draws(new ArrayList<>()).build();

            List<StageEntity> result = mapper.toEntityStages(List.of(s1, s2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrder()).isEqualTo(1);
            assertThat(result.get(1).getOrder()).isEqualTo(2);
        }

        @Test
        void toDomainDraws_should_map_list() {
            DrawEntity d1 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ELIMINATION").matches(new ArrayList<>()).build();
            DrawEntity d2 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ROUND_ROBIN").matches(new ArrayList<>()).build();

            List<Draw> result = mapper.toDomainDraws(List.of(d1, d2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDrawType()).isEqualTo(DrawType.ELIMINATION);
            assertThat(result.get(1).getDrawType()).isEqualTo(DrawType.ROUND_ROBIN);
        }

        @Test
        void toEntityDraws_should_map_list() {
            Draw d1 = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ELIMINATION).build();
            Draw d2 = Draw.builder().id(UUID.randomUUID()).drawType(DrawType.ROUND_ROBIN).build();

            List<DrawEntity> result = mapper.toEntityDraws(List.of(d1, d2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDrawType()).isEqualTo("ELIMINATION");
            assertThat(result.get(1).getDrawType()).isEqualTo("ROUND_ROBIN");
        }

        @Test
        void toDomainMatches_should_map_list() {
            MatchEntity m1 = MatchEntity.builder().id(UUID.randomUUID()).roundNumber(1).build();
            MatchEntity m2 = MatchEntity.builder().id(UUID.randomUUID()).roundNumber(2).build();

            List<Match> result = mapper.toDomainMatches(List.of(m1, m2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRoundNumber()).isEqualTo(1);
            assertThat(result.get(1).getRoundNumber()).isEqualTo(2);
        }
    }

    @Nested
    class ToPeriodTests {

        @Test
        void should_create_period_from_valid_dates() {
            LocalDate start = LocalDate.of(2026, 7, 1);
            LocalDate end = LocalDate.of(2026, 7, 10);

            TournamentPeriod period = mapper.toPeriod(start, end);

            assertThat(period).isNotNull();
            assertThat(period.startDate()).isEqualTo(start);
            assertThat(period.endDate()).isEqualTo(end);
        }

        @Test
        void should_return_null_when_start_date_is_null() {
            assertThat(mapper.toPeriod(null, LocalDate.of(2026, 7, 10))).isNull();
        }

        @Test
        void should_return_null_when_end_date_is_null() {
            assertThat(mapper.toPeriod(LocalDate.of(2026, 7, 1), null)).isNull();
        }

        @Test
        void should_return_null_when_both_dates_are_null() {
            assertThat(mapper.toPeriod(null, null)).isNull();
        }
    }

    @Nested
    class MapUuidTests {

        @Test
        void should_create_member_entity_from_uuid() {
            UUID uuid = UUID.randomUUID();

            MemberEntity result = mapper.map(uuid);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(uuid);
        }

        @Test
        void should_return_null_when_uuid_is_null() {
            assertThat(mapper.map((UUID) null)).isNull();
        }
    }

    @Nested
    class MapRefAgeCategoryTests {

        @Test
        void should_create_ref_age_category_from_integer() {
            RefAgeCategoryEntity result = mapper.mapRefAgeCategory(10);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10);
        }

        @Test
        void should_return_null_when_integer_is_null() {
            assertThat(mapper.mapRefAgeCategory(null)).isNull();
        }

        @Test
        void should_create_ref_age_category_with_zero() {
            RefAgeCategoryEntity result = mapper.mapRefAgeCategory(0);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(0);
        }
    }

    @Nested
    class LinkEventsToTournamentTests {

        @Test
        void should_link_events_to_tournament() {
            StageEntity stage = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .build();

            DrawEntity draw = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .build();

            stage.setDraws(List.of(draw));

            EventEntity event = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MALE")
                    .stages(List.of(stage))
                    .build();

            TournamentEntity tournament = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Link Test")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .surface(Surface.HARD)
                    .maxPlayers(32)
                    .location("Madrid")
                    .status(TournamentStatus.DRAFT)
                    .events(List.of(event))
                    .build();

            mapper.linkEventsToTournament(tournament);

            assertThat(event.getTournament()).isSameAs(tournament);
            assertThat(stage.getEvent()).isSameAs(event);
            assertThat(draw.getStage()).isSameAs(stage);
        }

        @Test
        void should_handle_null_events_gracefully() {
            TournamentEntity tournament = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .events(null)
                    .build();

            mapper.linkEventsToTournament(tournament);

            assertThat(tournament.getEvents()).isNull();
        }

        @Test
        void should_handle_event_with_null_stages() {
            EventEntity event = EventEntity.builder()
                    .id(EVENT_ID)
                    .stages(null)
                    .build();

            TournamentEntity tournament = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .events(List.of(event))
                    .build();

            mapper.linkEventsToTournament(tournament);

            assertThat(event.getTournament()).isSameAs(tournament);
        }

        @Test
        void should_handle_stage_with_null_draws() {
            StageEntity stage = StageEntity.builder()
                    .id(STAGE_ID)
                    .draws(null)
                    .build();

            EventEntity event = EventEntity.builder()
                    .id(EVENT_ID)
                    .stages(List.of(stage))
                    .build();

            TournamentEntity tournament = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .events(List.of(event))
                    .build();

            mapper.linkEventsToTournament(tournament);

            assertThat(stage.getEvent()).isSameAs(event);
        }

        @Test
        void should_link_multiple_events() {
            StageEntity stage1 = StageEntity.builder().id(UUID.randomUUID()).order(1).stageType("MAIN").build();
            EventEntity event1 = EventEntity.builder().id(UUID.randomUUID()).stages(List.of(stage1)).build();

            StageEntity stage2 = StageEntity.builder().id(UUID.randomUUID()).order(1).stageType("QUALIFYING").build();
            DrawEntity draw2 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ELIMINATION").build();
            stage2.setDraws(List.of(draw2));
            EventEntity event2 = EventEntity.builder().id(UUID.randomUUID()).stages(List.of(stage2)).build();

            TournamentEntity tournament = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .events(List.of(event1, event2))
                    .build();

            mapper.linkEventsToTournament(tournament);

            assertThat(event1.getTournament()).isSameAs(tournament);
            assertThat(event2.getTournament()).isSameAs(tournament);
            assertThat(stage1.getEvent()).isSameAs(event1);
            assertThat(stage2.getEvent()).isSameAs(event2);
            assertThat(draw2.getStage()).isSameAs(stage2);
        }

        @Test
        void should_handle_empty_events() {
            TournamentEntity tournament = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .events(new ArrayList<>())
                    .build();

            mapper.linkEventsToTournament(tournament);

            assertThat(tournament.getEvents()).isEmpty();
        }
    }

    @Nested
    class MapInscriptionDomainTests {

        @Test
        void should_create_inscription_from_uuid() {
            UUID inscriptionId = UUID.randomUUID();

            Inscription result = mapper.mapInscriptionDomain(inscriptionId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(inscriptionId);
            assertThat(result.getEventId()).isNull();
            assertThat(result.getParticipantId()).isNull();
            assertThat(result.getStatus()).isNull();
            assertThat(result.getPaymentStatus()).isNull();
            assertThat(result.getRegisteredAt()).isNull();
        }

        @Test
        void should_return_null_when_inscription_id_is_null() {
            assertThat(mapper.mapInscriptionDomain(null)).isNull();
        }

        @Test
        void should_create_stubs_for_all_inscription_fields() {
            UUID id = UUID.randomUUID();

            Inscription result = mapper.mapInscriptionDomain(id);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getParticipantSource()).isNull();
            assertThat(result.getSeed()).isNull();
            assertThat(result.getProfessionalRankingPosition()).isNull();
            assertThat(result.getProfessionalAwardedPoints()).isNull();
        }
    }

    @Nested
    class EventEntityMappingTests {

        @Test
        void should_return_null_when_event_entity_is_null() {
            assertThat(mapper.toDomain((EventEntity) null)).isNull();
        }

        @Test
        void should_return_null_when_event_domain_is_null() {
            assertThat(mapper.toEntity((Event) null)).isNull();
        }

        @Test
        void should_map_event_with_multiple_stages() {
            StageEntity s1 = StageEntity.builder().id(UUID.randomUUID()).order(1).stageType("ROUND_ROBIN").build();
            StageEntity s2 = StageEntity.builder().id(UUID.randomUUID()).order(2).stageType("MAIN").build();

            EventEntity entity = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("MIXED")
                    .ageCategory(RefAgeCategoryEntity.builder().id(8).build())
                    .stages(List.of(s1, s2))
                    .build();

            Event domain = mapper.toDomain(entity);

            assertThat(domain.getStages()).hasSize(2);
            assertThat(domain.getStages().get(0).getStageNumber()).isEqualTo(1);
            assertThat(domain.getStages().get(1).getStageNumber()).isEqualTo(2);
        }
    }

    @Nested
    class StageEntityMappingTests {

        @Test
        void should_return_null_when_stage_entity_is_null() {
            assertThat(mapper.toDomain((StageEntity) null)).isNull();
        }

        @Test
        void should_return_null_when_stage_domain_is_null() {
            assertThat(mapper.toEntity((Stage) null)).isNull();
        }

        @Test
        void should_map_stage_with_multiple_draws() {
            DrawEntity d1 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ELIMINATION").build();
            DrawEntity d2 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ROUND_ROBIN").build();

            StageEntity entity = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .draws(List.of(d1, d2))
                    .build();

            Stage domain = mapper.toDomain(entity);

            assertThat(domain.getDraws()).hasSize(2);
            assertThat(domain.getDraws().get(0).getDrawType()).isEqualTo(DrawType.ELIMINATION);
            assertThat(domain.getDraws().get(1).getDrawType()).isEqualTo(DrawType.ROUND_ROBIN);
        }
    }

    @Nested
    class DrawEntityMappingTests {

        @Test
        void should_return_null_when_draw_entity_is_null() {
            assertThat(mapper.toDomain((DrawEntity) null)).isNull();
        }

        @Test
        void should_return_null_when_draw_domain_is_null() {
            assertThat(mapper.toEntity((Draw) null)).isNull();
        }
    }

    @Nested
    class MatchEntityMappingTests {

        @Test
        void should_return_null_when_match_entity_is_null() {
            assertThat(mapper.toDomain((MatchEntity) null)).isNull();
        }

        @Test
        void should_map_match_with_all_null_optional_fields() {
            MatchEntity entity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .draw(null)
                    .firstInscription(null)
                    .secondInscription(null)
                    .winner(null)
                    .nextMatch(null)
                    .loserNextMatch(null)
                    .courtResource(null)
                    .court(null)
                    .result(null)
                    .scheduledAt(null)
                    .scheduleTimeType(null)
                    .roundNumber(3)
                    .bracketPosition(5)
                    .build();

            Match match = mapper.toDomain(entity);

            assertThat(match.getId()).isEqualTo(MATCH_ID);
            assertThat(match.getDrawId()).isNull();
            assertThat(match.getFirstInscription()).isNull();
            assertThat(match.getSecondInscription()).isNull();
            assertThat(match.getWinner()).isNull();
            assertThat(match.getNextMatch()).isNull();
            assertThat(match.getLoserNextMatch()).isNull();
            assertThat(match.getCourtId()).isNull();
            assertThat(match.getCourt()).isNull();
            assertThat(match.getResult()).isNull();
            assertThat(match.getScheduledAt()).isNull();
            assertThat(match.getScheduleTimeType()).isNull();
            assertThat(match.getRoundNumber()).isEqualTo(3);
            assertThat(match.getBracketPosition()).isEqualTo(5);
        }

        @Test
        void should_map_match_with_schedule_time_type_not_before() {
            MatchEntity entity = MatchEntity.builder()
                    .id(MATCH_ID)
                    .scheduleTimeType(ScheduleTimeType.NOT_BEFORE)
                    .roundNumber(1)
                    .build();

            Match match = mapper.toDomain(entity);

            assertThat(match.getScheduleTimeType()).isEqualTo(ScheduleTimeType.NOT_BEFORE);
        }
    }

    @Nested
    class FullRoundTripTests {

        @Test
        void should_round_trip_tournament_entity_to_domain_and_back() {
            MemberEntity createdBy = MemberEntity.builder().id(CREATED_BY_ID).build();
            TournamentEntity original = TournamentEntity.builder()
                    .id(TOURNAMENT_ID)
                    .formalName("Round Trip Tournament")
                    .playStartDate(LocalDate.of(2026, 7, 1))
                    .playEndDate(LocalDate.of(2026, 7, 10))
                    .startTime(LocalTime.of(9, 0))
                    .inscriptionStartDate(LocalDate.of(2026, 6, 1))
                    .inscriptionEndDate(LocalDate.of(2026, 6, 25))
                    .surface(Surface.HARD)
                    .maxPlayers(64)
                    .location("Madrid")
                    .status(TournamentStatus.OPEN)
                    .createdBy(createdBy)
                    .events(new ArrayList<>())
                    .build();

            Tournament domain = mapper.toDomain(original);
            TournamentEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(original.getId());
            assertThat(result.getFormalName()).isEqualTo(original.getFormalName());
            assertThat(result.getPlayStartDate()).isEqualTo(original.getPlayStartDate());
            assertThat(result.getPlayEndDate()).isEqualTo(original.getPlayEndDate());
            assertThat(result.getStartTime()).isEqualTo(original.getStartTime());
            assertThat(result.getInscriptionStartDate()).isEqualTo(original.getInscriptionStartDate());
            assertThat(result.getInscriptionEndDate()).isEqualTo(original.getInscriptionEndDate());
            assertThat(result.getSurface()).isEqualTo(original.getSurface());
            assertThat(result.getMaxPlayers()).isEqualTo(original.getMaxPlayers());
            assertThat(result.getLocation()).isEqualTo(original.getLocation());
            assertThat(result.getStatus()).isEqualTo(original.getStatus());
        }

        @Test
        void should_round_trip_event_entity_to_domain_and_back() {
            EventEntity original = EventEntity.builder()
                    .id(EVENT_ID)
                    .gender("FEMALE")
                    .ageCategory(RefAgeCategoryEntity.builder().id(5).build())
                    .stages(new ArrayList<>())
                    .build();

            Event domain = mapper.toDomain(original);
            EventEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(original.getId());
            assertThat(result.getGender()).isEqualTo(original.getGender());
            assertThat(result.getAgeCategory().getId()).isEqualTo(original.getAgeCategory().getId());
        }

        @Test
        void should_round_trip_stage_entity_to_domain_and_back() {
            StageEntity original = StageEntity.builder()
                    .id(STAGE_ID)
                    .order(1)
                    .stageType("MAIN")
                    .strategyName("single_elimination")
                    .description("Main stage")
                    .draws(new ArrayList<>())
                    .build();

            Stage domain = mapper.toDomain(original);
            StageEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(original.getId());
            assertThat(result.getOrder()).isEqualTo(original.getOrder());
            assertThat(result.getStageType()).isEqualTo(original.getStageType());
            assertThat(result.getStrategyName()).isEqualTo(original.getStrategyName());
            assertThat(result.getDescription()).isEqualTo(original.getDescription());
        }

        @Test
        void should_round_trip_draw_entity_to_domain_and_back() {
            DrawEntity original = DrawEntity.builder()
                    .id(DRAW_ID)
                    .drawType("ELIMINATION")
                    .label("Main Draw")
                    .groupIndex(0)
                    .matches(new ArrayList<>())
                    .build();

            Draw domain = mapper.toDomain(original);
            DrawEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(original.getId());
            assertThat(result.getDrawType()).isEqualTo(original.getDrawType());
            assertThat(result.getLabel()).isEqualTo(original.getLabel());
        }
    }
}
