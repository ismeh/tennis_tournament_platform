package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.StageEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.StageDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaStageRepository;
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
class StageRepositoryAdapterTest {

    @Mock
    private JpaStageRepository stageRepository;
    @Mock
    private StageDomainMapper mapper;
    @InjectMocks
    private StageRepositoryAdapter adapter;

    @Test
    void should_save_stage() {
        UUID id = UUID.randomUUID();
        Stage domain = Stage.builder().id(id).stageNumber(1).stageType(StageType.MAIN).build();
        StageEntity entity = StageEntity.builder().id(id).order(1).stageType("MAIN").build();
        StageEntity savedEntity = StageEntity.builder().id(id).order(1).stageType("MAIN").build();
        Stage mapped = Stage.builder().id(id).stageNumber(1).stageType(StageType.MAIN).build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(stageRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mapped);

        Stage result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
    }

    @Test
    void should_find_by_id() {
        UUID id = UUID.randomUUID();
        StageEntity entity = StageEntity.builder().id(id).order(1).build();
        Stage mapped = Stage.builder().id(id).stageNumber(1).stageType(StageType.MAIN).build();

        when(stageRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        Optional<Stage> result = adapter.findById(id);

        assertThat(result).contains(mapped);
    }

    @Test
    void should_return_empty_when_not_found() {
        UUID id = UUID.randomUUID();
        when(stageRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    void should_find_by_event_id() {
        UUID eventId = UUID.randomUUID();
        StageEntity e1 = StageEntity.builder().id(UUID.randomUUID()).order(1).build();
        Stage d1 = Stage.builder().id(e1.getId()).stageNumber(1).stageType(StageType.MAIN).build();

        when(stageRepository.findByEvent_Id(eventId)).thenReturn(List.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        List<Stage> result = adapter.findByEventId(eventId);

        assertThat(result).hasSize(1).containsExactly(d1);
    }
}
