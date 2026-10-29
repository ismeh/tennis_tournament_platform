package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ClubDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaClubRepository;
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
class ClubRepositoryAdapterTest {

    @Mock
    private JpaClubRepository clubRepository;
    @Mock
    private ClubDomainMapper mapper;
    @InjectMocks
    private ClubRepositoryAdapter adapter;

    @Test
    void should_find_by_name_containing() {
        String query = "tennis";
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ClubEntity e1 = ClubEntity.builder().id(id1).name("Madrid Tennis Club").build();
        ClubEntity e2 = ClubEntity.builder().id(id2).name("Barcelona Tennis Academy").build();
        Club d1 = Club.builder().id(id1).name("Madrid Tennis Club").build();
        Club d2 = Club.builder().id(id2).name("Barcelona Tennis Academy").build();

        when(clubRepository.findByNameContaining(query)).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        List<Club> result = adapter.findByNameContaining(query);

        assertThat(result).hasSize(2).containsExactly(d1, d2);
        verify(clubRepository).findByNameContaining(query);
    }

    @Test
    void should_return_empty_list_when_no_name_matches() {
        when(clubRepository.findByNameContaining("xyz")).thenReturn(List.of());

        List<Club> result = adapter.findByNameContaining("xyz");

        assertThat(result).isEmpty();
    }

    @Test
    void should_find_by_name_ignore_case() {
        String name = "Madrid Tennis Club";
        UUID id = UUID.randomUUID();
        ClubEntity entity = ClubEntity.builder().id(id).name("Madrid Tennis Club").build();
        Club domain = Club.builder().id(id).name("Madrid Tennis Club").build();

        when(clubRepository.findByNameIgnoreCase(name)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Club> result = adapter.findByNameIgnoreCase(name);

        assertThat(result).contains(domain);
    }

    @Test
    void should_return_empty_when_name_not_found() {
        when(clubRepository.findByNameIgnoreCase("Nonexistent")).thenReturn(Optional.empty());

        assertThat(adapter.findByNameIgnoreCase("Nonexistent")).isEmpty();
    }

    @Test
    void should_save_club() {
        UUID id = UUID.randomUUID();
        Club domain = Club.builder().id(id).name("Madrid Tennis Club").build();
        ClubEntity entity = ClubEntity.builder().id(id).name("Madrid Tennis Club").build();
        ClubEntity savedEntity = ClubEntity.builder().id(id).name("Madrid Tennis Club").build();
        Club mapped = Club.builder().id(id).name("Madrid Tennis Club").build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(clubRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mapped);

        Club result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
        verify(mapper).toEntity(domain);
        verify(clubRepository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }
}
