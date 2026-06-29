package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrawDomainMapperTest {

    @Mock
    private MatchDomainMapper matchDomainMapper;
    @InjectMocks
    private DrawDomainMapper mapper;

    @Nested
    class ToDomainTests {
        @Test
        void should_return_null_when_entity_is_null() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        void should_map_basic_fields() {
            UUID stageId = UUID.randomUUID();
            StageEntity stage = StageEntity.builder().id(stageId).build();
            DrawEntity entity = DrawEntity.builder()
                    .id(UUID.randomUUID())
                    .stage(stage)
                    .drawType("ELIMINATION")
                    .label("Main Draw")
                    .groupIndex(0)
                    .build();

            Draw result = mapper.toDomain(entity);

            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getStageId()).isEqualTo(stageId);
            assertThat(result.getDrawType()).isEqualTo(DrawType.ELIMINATION);
            assertThat(result.getLabel()).isEqualTo("Main Draw");
            assertThat(result.getDrawName()).isEqualTo("Main Draw");
            assertThat(result.getGroupIndex()).isEqualTo(0);
        }

        @Test
        void should_handle_null_stage() {
            DrawEntity entity = DrawEntity.builder()
                    .id(UUID.randomUUID())
                    .drawType("ROUND_ROBIN")
                    .build();

            Draw result = mapper.toDomain(entity);

            assertThat(result.getStageId()).isNull();
        }

        @Test
        void should_throw_when_draw_type_is_null() {
            DrawEntity entity = DrawEntity.builder()
                    .id(UUID.randomUUID())
                    .drawType(null)
                    .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_map_matches() {
            DrawEntity entity = DrawEntity.builder()
                    .id(UUID.randomUUID())
                    .drawType("ELIMINATION")
                    .build();

            when(matchDomainMapper.toDomainList(anyList())).thenReturn(List.of());

            Draw result = mapper.toDomain(entity);

            verify(matchDomainMapper).toDomainList(anyList());
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
            UUID stageId = UUID.randomUUID();
            Draw domain = Draw.builder()
                    .id(UUID.randomUUID())
                    .stageId(stageId)
                    .drawType(DrawType.ROUND_ROBIN)
                    .label("Group A")
                    .groupIndex(1)
                    .build();

            DrawEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            assertThat(result.getStage().getId()).isEqualTo(stageId);
            assertThat(result.getDrawType()).isEqualTo("ROUND_ROBIN");
            assertThat(result.getLabel()).isEqualTo("Group A");
            assertThat(result.getGroupIndex()).isEqualTo(1);
        }

        @Test
        void should_use_draw_name_as_fallback_for_label() {
            Draw domain = Draw.builder()
                    .id(UUID.randomUUID())
                    .drawType(DrawType.ELIMINATION)
                    .label(null)
                    .drawName("Fallback Label")
                    .build();

            DrawEntity result = mapper.toEntity(domain);

            assertThat(result.getLabel()).isEqualTo("Fallback Label");
        }

        @Test
        void should_handle_null_stage_id() {
            Draw domain = Draw.builder()
                    .id(UUID.randomUUID())
                    .drawType(DrawType.ELIMINATION)
                    .build();

            DrawEntity result = mapper.toEntity(domain);

            assertThat(result.getStage()).isNull();
        }
    }

    @Nested
    class ListTests {
        @Test
        void should_return_empty_when_domain_list_is_null() {
            assertThat(mapper.toEntityList(null)).isEmpty();
        }

        @Test
        void should_return_empty_when_entity_list_is_null() {
            assertThat(mapper.toDomainList(null)).isEmpty();
        }

        @Test
        void should_map_entity_list() {
            DrawEntity e1 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ELIMINATION").build();
            when(matchDomainMapper.toDomainList(anyList())).thenReturn(List.of());

            List<Draw> result = mapper.toDomainList(List.of(e1));

            assertThat(result).hasSize(1);
        }
    }
}
