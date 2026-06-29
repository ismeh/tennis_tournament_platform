package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.InscriptionMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
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
class InscriptionRepositoryAdapterTest {

    @Mock
    private JpaInscriptionRepository inscriptionRepository;
    @Mock
    private InscriptionMapper mapper;
    @InjectMocks
    private InscriptionRepositoryAdapter adapter;

    @Test
    void should_save_inscription() {
        UUID id = UUID.randomUUID();
        Inscription domain = Inscription.builder().id(id).status("CONFIRMED").build();
        InscriptionEntity entity = InscriptionEntity.builder().id(id).status("CONFIRMED").build();
        InscriptionEntity savedEntity = InscriptionEntity.builder().id(id).status("CONFIRMED").build();
        Inscription mapped = Inscription.builder().id(id).status("CONFIRMED").build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(inscriptionRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mapped);

        Inscription result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
    }

    @Test
    void should_find_by_tournament_id() {
        UUID tournamentId = UUID.randomUUID();
        InscriptionEntity e1 = InscriptionEntity.builder().id(UUID.randomUUID()).build();
        Inscription d1 = Inscription.builder().id(e1.getId()).build();

        when(inscriptionRepository.findByEvent_Tournament_Id(tournamentId)).thenReturn(List.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        List<Inscription> result = adapter.findByTournamentId(tournamentId);

        assertThat(result).hasSize(1).containsExactly(d1);
    }

    @Test
    void should_find_by_event_id() {
        UUID eventId = UUID.randomUUID();
        InscriptionEntity e1 = InscriptionEntity.builder().id(UUID.randomUUID()).build();
        Inscription d1 = Inscription.builder().id(e1.getId()).build();

        when(inscriptionRepository.findByEvent_Id(eventId)).thenReturn(List.of(e1));
        when(mapper.toDomain(e1)).thenReturn(d1);

        List<Inscription> result = adapter.findByEventId(eventId);

        assertThat(result).hasSize(1).containsExactly(d1);
    }
}
