package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ProPlayerMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaProPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProPlayerRepositoryAdapterTest {

    @Mock
    private JpaProPlayerRepository jpaProPlayerRepository;
    @Mock
    private ProPlayerMapper proPlayerMapper;
    @InjectMocks
    private ProPlayerRepositoryAdapter adapter;

    @Test
    void should_find_by_id() {
        ProPlayerEntity entity = ProPlayerEntity.builder().id(1).name("Rafael Nadal").build();
        ProPlayer domain = ProPlayer.builder().id(1).fullName("Rafael Nadal").build();

        when(jpaProPlayerRepository.findById(1)).thenReturn(Optional.of(entity));
        when(proPlayerMapper.toDomain(entity)).thenReturn(domain);

        Optional<ProPlayer> result = adapter.findById(1);

        assertThat(result).contains(domain);
    }

    @Test
    void should_return_empty_when_not_found() {
        when(jpaProPlayerRepository.findById(1)).thenReturn(Optional.empty());

        assertThat(adapter.findById(1)).isEmpty();
    }

    @Test
    void should_find_by_license() {
        ProPlayerEntity entity = ProPlayerEntity.builder().id(1).license("L001").name("Player").build();
        ProPlayer domain = ProPlayer.builder().id(1).license("L001").build();

        when(jpaProPlayerRepository.findFirstByLicenseIgnoreCase("L001")).thenReturn(Optional.of(entity));
        when(proPlayerMapper.toDomain(entity)).thenReturn(domain);

        Optional<ProPlayer> result = adapter.findByLicense("L001");

        assertThat(result).contains(domain);
    }

    @Test
    void should_return_empty_when_license_is_null() {
        assertThat(adapter.findByLicense(null)).isEmpty();
    }

    @Test
    void should_return_empty_when_license_is_blank() {
        assertThat(adapter.findByLicense("   ")).isEmpty();
    }

    @Test
    void should_find_top10() {
        ProPlayerEntity e1 = ProPlayerEntity.builder().id(1).name("A").build();
        ProPlayer d1 = ProPlayer.builder().id(1).fullName("A").build();

        when(jpaProPlayerRepository.findTop10ByOrderByRankingPositionAsc()).thenReturn(List.of(e1));
        when(proPlayerMapper.toDomain(e1)).thenReturn(d1);

        List<ProPlayer> result = adapter.findTop10();

        assertThat(result).hasSize(1).containsExactly(d1);
    }

    @Test
    void should_search_by_query() {
        ProPlayerEntity e1 = ProPlayerEntity.builder().id(1).name("Nadal").build();
        ProPlayer d1 = ProPlayer.builder().id(1).fullName("Nadal").build();

        when(jpaProPlayerRepository.search(eq("Nadal"), any(), any(), any())).thenReturn(List.of(e1));
        when(proPlayerMapper.toDomain(e1)).thenReturn(d1);

        List<ProPlayer> result = adapter.searchByQuery("Nadal");

        assertThat(result).hasSize(1).containsExactly(d1);
    }

    @Test
    void should_search_with_filters() {
        ProPlayerEntity e1 = ProPlayerEntity.builder().id(1).name("Player").build();
        ProPlayer d1 = ProPlayer.builder().id(1).fullName("Player").build();

        when(jpaProPlayerRepository.search(eq("query"), eq("M"), eq("30+"), any())).thenReturn(List.of(e1));
        when(proPlayerMapper.toDomain(e1)).thenReturn(d1);

        List<ProPlayer> result = adapter.search("query", "M", "30+");

        assertThat(result).hasSize(1).containsExactly(d1);
    }
}
