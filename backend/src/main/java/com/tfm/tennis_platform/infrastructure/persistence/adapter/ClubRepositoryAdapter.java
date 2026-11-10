package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.domain.port.out.ClubRepository;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ClubDomainMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClubRepositoryAdapter implements ClubRepository {

    private final JpaClubRepository clubRepository;
    private final ClubDomainMapper mapper;

    @Override
    public List<Club> findByNameContaining(String query) {
        return clubRepository.findByNameContaining(query).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Club> findPublicClubs(String query) {
        return clubRepository.findPublicClubs(query).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Club> findByNameIgnoreCase(String name) {
        return clubRepository.findByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public Optional<Club> findById(UUID id) {
        return clubRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Club> findBySlug(String slug) {
        return clubRepository.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return clubRepository.existsBySlug(slug);
    }

    @Override
    public Club save(Club club) {
        return mapper.toDomain(clubRepository.save(mapper.toEntity(club)));
    }
}
