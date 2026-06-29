package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.EventEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StageDomainMapperTest {

    @Mock
    private DrawDomainMapper drawDomainMapper;
    @InjectMocks
    private StageDomainMapper mapper;

    @Nested
    class ToDomainTests {
        @Test
        void should_return_null_when_entity_is_null() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        void should_map_basic_fields() {
            UUID eventId = UUID.randomUUID();
            EventEntity event = EventEntity.builder().id(eventId).build();
            StageEntity entity = StageEntity.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .order(1)
                    .stageType("MAIN")
                    .strategyName("standard")
                    .description("Main stage")
                    .build();

            Stage result = mapper.toDomain(entity);

            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getEventId()).isEqualTo(eventId);
            assertThat(result.getStageNumber()).isEqualTo(1);
            assertThat(result.getStageType()).isEqualTo(StageType.MAIN);
            assertThat(result.getStrategyName()).isEqualTo("standard");
            assertThat(result.getDescription()).isEqualTo("Main stage");
        }

        @Test
        void should_handle_null_event() {
            StageEntity entity = StageEntity.builder()
                    .id(UUID.randomUUID())
                    .order(1)
                    .stageType("ROUND_ROBIN")
                    .build();

            Stage result = mapper.toDomain(entity);

            assertThat(result.getEventId()).isNull();
        }

        @Test
        void should_throw_when_stage_type_is_null() {
            StageEntity entity = StageEntity.builder()
                    .id(UUID.randomUUID())
                    .order(1)
                    .stageType(null)
                    .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_map_draws() {
            UUID drawId = UUID.randomUUID();
            DrawEntity drawEntity = DrawEntity.builder().id(drawId).build();
            StageEntity entity = StageEntity.builder()
                    .id(UUID.randomUUID())
                    .order(1)
                    .stageType("MAIN")
                    .draws(List.of(drawEntity))
                    .build();

            when(drawDomainMapper.toDomain(drawEntity)).thenReturn(
                    com.tfm.tennis_platform.domain.models.Draw.builder()
                            .id(drawId)
                            .drawType(com.tfm.tennis_platform.domain.models.enums.DrawType.ELIMINATION)
                            .build()
            );

            Stage result = mapper.toDomain(entity);

            assertThat(result.getDraws()).hasSize(1);
        }

        @Test
        void should_handle_null_draws() {
            StageEntity entity = StageEntity.builder()
                    .id(UUID.randomUUID())
                    .order(1)
                    .stageType("MAIN")
                    .draws(null)
                    .build();

            Stage result = mapper.toDomain(entity);

            assertThat(result.getDraws()).isEmpty();
        }
    }

    @Nested
    class ToEntityTests {
        @Test
        void should_return_null_when_domain_is_null() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        void should_map_basic_fields() {
            UUID eventId = UUID.randomUUID();
            Stage domain = Stage.builder()
                    .id(UUID.randomUUID())
                    .eventId(eventId)
                    .stageNumber(1)
                    .stageType(StageType.ROUND_ROBIN)
                    .strategyName("round_robin")
                    .description("Groups")
                    .build();

            StageEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            assertThat(result.getEvent().getId()).isEqualTo(eventId);
            assertThat(result.getOrder()).isEqualTo(1);
            assertThat(result.getStageType()).isEqualTo("ROUND_ROBIN");
            assertThat(result.getStrategyName()).isEqualTo("round_robin");
            assertThat(result.getDescription()).isEqualTo("Groups");
        }

        @Test
        void should_handle_null_event_id() {
            Stage domain = Stage.builder()
                    .id(UUID.randomUUID())
                    .stageNumber(1)
                    .stageType(StageType.MAIN)
                    .build();

            StageEntity result = mapper.toEntity(domain);

            assertThat(result.getEvent()).isNull();
        }

        @Test
        void should_map_draws_and_set_back_reference() {
            UUID stageId = UUID.randomUUID();
            UUID drawId = UUID.randomUUID();
            com.tfm.tennis_platform.domain.models.Draw drawDomain =
                    com.tfm.tennis_platform.domain.models.Draw.builder()
                            .id(drawId)
                            .drawType(com.tfm.tennis_platform.domain.models.enums.DrawType.ELIMINATION)
                            .build();
            Stage domain = Stage.builder()
                    .id(stageId)
                    .stageNumber(1)
                    .stageType(StageType.MAIN)
                    .draw(drawDomain)
                    .build();

            DrawEntity drawEntity = DrawEntity.builder().id(drawId).build();
            when(drawDomainMapper.toEntity(drawDomain)).thenReturn(drawEntity);

            StageEntity result = mapper.toEntity(domain);

            assertThat(result.getDraws()).hasSize(1);
            assertThat(result.getDraws().get(0).getStage()).isSameAs(result);
        }
    }

    @Nested
    class ListTests {
        @Test
        void should_return_empty_list_when_domain_list_is_null() {
            assertThat(mapper.toEntityList(null)).isEmpty();
        }

        @Test
        void should_return_empty_list_when_entity_list_is_null() {
            assertThat(mapper.toDomainList(null)).isEmpty();
        }

        @Test
        void should_map_entity_list() {
            UUID eventId = UUID.randomUUID();
            StageEntity e1 = StageEntity.builder().id(UUID.randomUUID()).order(1).stageType("MAIN").event(EventEntity.builder().id(eventId).build()).build();
            StageEntity e2 = StageEntity.builder().id(UUID.randomUUID()).order(2).stageType("ROUND_ROBIN").event(EventEntity.builder().id(eventId).build()).build();

            List<Stage> result = mapper.toDomainList(List.of(e1, e2));

            assertThat(result).hasSize(2);
        }
    }
}
