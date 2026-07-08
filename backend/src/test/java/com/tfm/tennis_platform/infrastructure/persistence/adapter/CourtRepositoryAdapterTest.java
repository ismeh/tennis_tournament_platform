package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.infrastructure.persistence.entity.CourtEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.CourtDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaCourtRepository;
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
class CourtRepositoryAdapterTest {

    @Mock
    private JpaCourtRepository courtRepository;
    @Mock
    private CourtDomainMapper mapper;
    @InjectMocks
    private CourtRepositoryAdapter adapter;

    @Test
    void should_save_court() {
        UUID id = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Court domain = Court.builder().id(id).tournamentId(tournamentId).name("Court 1").active(true).build();
        CourtEntity entity = CourtEntity.builder().id(id).name("Court 1").active(true).build();
        CourtEntity savedEntity = CourtEntity.builder().id(id).name("Court 1").active(true).build();
        Court mapped = Court.builder().id(id).tournamentId(tournamentId).name("Court 1").active(true).build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(courtRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mapped);

        Court result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
        verify(mapper).toEntity(domain);
        verify(courtRepository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void should_find_by_tournament_id() {
        UUID tournamentId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CourtEntity e1 = CourtEntity.builder().id(id1).name("Court 1").active(true).build();
        CourtEntity e2 = CourtEntity.builder().id(id2).name("Court 2").active(false).build();
        Court d1 = Court.builder().id(id1).tournamentId(tournamentId).name("Court 1").active(true).build();
        Court d2 = Court.builder().id(id2).tournamentId(tournamentId).name("Court 2").active(false).build();

        when(courtRepository.findByTournamentIdOrderByNameAsc(tournamentId)).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        List<Court> result = adapter.findByTournamentId(tournamentId);

        assertThat(result).hasSize(2).containsExactly(d1, d2);
        verify(courtRepository).findByTournamentIdOrderByNameAsc(tournamentId);
    }

    @Test
    void should_return_empty_list_when_no_courts_for_tournament() {
        UUID tournamentId = UUID.randomUUID();
        when(courtRepository.findByTournamentIdOrderByNameAsc(tournamentId)).thenReturn(List.of());

        List<Court> result = adapter.findByTournamentId(tournamentId);

        assertThat(result).isEmpty();
    }

    @Test
    void should_find_by_id_and_tournament_id() {
        UUID id = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        CourtEntity entity = CourtEntity.builder().id(id).name("Court 1").active(true).build();
        Court domain = Court.builder().id(id).tournamentId(tournamentId).name("Court 1").active(true).build();

        when(courtRepository.findByIdAndTournamentId(id, tournamentId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Court> result = adapter.findByIdAndTournamentId(id, tournamentId);

        assertThat(result).contains(domain);
    }

    @Test
    void should_return_empty_when_court_not_found_for_tournament() {
        UUID id = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        when(courtRepository.findByIdAndTournamentId(id, tournamentId)).thenReturn(Optional.empty());

        assertThat(adapter.findByIdAndTournamentId(id, tournamentId)).isEmpty();
    }

    @Test
    void should_check_exists_by_tournament_id_and_name() {
        UUID tournamentId = UUID.randomUUID();
        String name = "Court 1";
        when(courtRepository.existsByTournamentIdAndNameIgnoreCase(tournamentId, name)).thenReturn(true);

        boolean result = adapter.existsByTournamentIdAndName(tournamentId, name);

        assertThat(result).isTrue();
        verify(courtRepository).existsByTournamentIdAndNameIgnoreCase(tournamentId, name);
    }

    @Test
    void should_return_false_when_court_name_not_exists() {
        UUID tournamentId = UUID.randomUUID();
        when(courtRepository.existsByTournamentIdAndNameIgnoreCase(tournamentId, "Nonexistent")).thenReturn(false);

        boolean result = adapter.existsByTournamentIdAndName(tournamentId, "Nonexistent");

        assertThat(result).isFalse();
    }

    @Test
    void should_delete_by_id() {
        UUID id = UUID.randomUUID();
        doNothing().when(courtRepository).deleteById(id);

        adapter.deleteById(id);

        verify(courtRepository).deleteById(id);
    }
}
