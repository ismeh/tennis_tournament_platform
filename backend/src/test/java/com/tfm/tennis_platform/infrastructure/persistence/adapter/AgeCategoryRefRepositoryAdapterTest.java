package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;
import com.tfm.tennis_platform.infrastructure.persistence.entity.RefAgeCategoryEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.AgeCategoryRefMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.RefAgeCategoryRepository;
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
class AgeCategoryRefRepositoryAdapterTest {

    @Mock
    private RefAgeCategoryRepository repository;
    @Mock
    private AgeCategoryRefMapper mapper;
    @InjectMocks
    private AgeCategoryRefRepositoryAdapter adapter;

    @Test
    void should_find_all() {
        RefAgeCategoryEntity e1 = RefAgeCategoryEntity.builder().id(1).category("30+").build();
        AgeCategoryRef d1 = AgeCategoryRef.builder().id(1).category("30+").build();

        when(repository.findAllOrdered()).thenReturn(List.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        List<AgeCategoryRef> result = adapter.findAll();

        assertThat(result).hasSize(1).containsExactly(d1);
    }

    @Test
    void should_find_by_organizer_id() {
        UUID orgId = UUID.randomUUID();
        RefAgeCategoryEntity e1 = RefAgeCategoryEntity.builder().id(1).category("30+").organizerId(orgId).build();
        AgeCategoryRef d1 = AgeCategoryRef.builder().id(1).category("30+").organizerId(orgId).build();

        when(repository.findByOrganizerIdOrdered(orgId)).thenReturn(List.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        List<AgeCategoryRef> result = adapter.findByOrganizerId(orgId);

        assertThat(result).hasSize(1).containsExactly(d1);
    }

    @Test
    void should_find_by_id() {
        RefAgeCategoryEntity e1 = RefAgeCategoryEntity.builder().id(1).category("30+").build();
        AgeCategoryRef d1 = AgeCategoryRef.builder().id(1).category("30+").build();

        when(repository.findById(1)).thenReturn(Optional.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        Optional<AgeCategoryRef> result = adapter.findById(1);

        assertThat(result).contains(d1);
    }

    @Test
    void should_save() {
        AgeCategoryRef domain = AgeCategoryRef.builder().category("30+").build();
        RefAgeCategoryEntity entity = RefAgeCategoryEntity.builder().category("30+").build();
        RefAgeCategoryEntity saved = RefAgeCategoryEntity.builder().id(1).category("30+").build();
        AgeCategoryRef mapped = AgeCategoryRef.builder().id(1).category("30+").build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(mapped);

        AgeCategoryRef result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
    }

    @Test
    void should_delete_by_id() {
        adapter.deleteById(1);
        verify(repository).deleteById(1);
    }

    @Test
    void should_check_exists() {
        UUID orgId = UUID.randomUUID();
        when(repository.existsByOrganizerIdAndCategoryIgnoreCase(orgId, "30+")).thenReturn(true);

        boolean result = adapter.existsByOrganizerIdAndCategory(orgId, "30+");

        assertThat(result).isTrue();
    }
}
