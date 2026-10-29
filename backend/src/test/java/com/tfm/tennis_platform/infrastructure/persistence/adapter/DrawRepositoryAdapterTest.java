package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.DrawEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.DrawDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaDrawRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrawRepositoryAdapterTest {

    @Mock
    private JpaDrawRepository drawRepository;
    @Mock
    private DrawDomainMapper mapper;
    @InjectMocks
    private DrawRepositoryAdapter adapter;

    @Test
    void should_save_draw() {
        UUID id = UUID.randomUUID();
        Draw domain = Draw.builder().id(id).drawType(DrawType.ELIMINATION).build();
        DrawEntity entity = DrawEntity.builder().id(id).drawType("ELIMINATION").build();
        DrawEntity savedEntity = DrawEntity.builder().id(id).drawType("ELIMINATION").build();
        Draw mapped = Draw.builder().id(id).drawType(DrawType.ELIMINATION).build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(drawRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mapped);

        Draw result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
        verify(mapper).toEntity(domain);
        verify(drawRepository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void should_find_by_id() {
        UUID id = UUID.randomUUID();
        DrawEntity entity = DrawEntity.builder().id(id).drawType("ELIMINATION").build();
        Draw mapped = Draw.builder().id(id).drawType(DrawType.ELIMINATION).build();

        when(drawRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        Optional<Draw> result = adapter.findById(id);

        assertThat(result).contains(mapped);
    }

    @Test
    void should_return_empty_when_not_found() {
        UUID id = UUID.randomUUID();
        when(drawRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    void should_find_by_stage_id() {
        UUID stageId = UUID.randomUUID();
        DrawEntity e1 = DrawEntity.builder().id(UUID.randomUUID()).drawType("ELIMINATION").build();
        Draw d1 = Draw.builder().id(e1.getId()).drawType(DrawType.ELIMINATION).build();

        when(drawRepository.findByStage_Id(stageId)).thenReturn(List.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        List<Draw> result = adapter.findByStageId(stageId);

        assertThat(result).hasSize(1).containsExactly(d1);
    }
}
