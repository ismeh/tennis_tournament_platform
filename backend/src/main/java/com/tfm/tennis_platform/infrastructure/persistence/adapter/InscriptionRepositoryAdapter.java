package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.InscriptionMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InscriptionRepositoryAdapter implements InscriptionRepository {

    private final JpaInscriptionRepository inscriptionRepository;
    private final InscriptionMapper mapper;

    @Override
    public Inscription save(Inscription inscription) {
        return mapper.toDomain(inscriptionRepository.save(mapper.toEntity(inscription)));
    }

    @Override
    public List<Inscription> findByTournamentId(UUID tournamentId) {
        return inscriptionRepository.findByEvent_Tournament_Id(tournamentId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
