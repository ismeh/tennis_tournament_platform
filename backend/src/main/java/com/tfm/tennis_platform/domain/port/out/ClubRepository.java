package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Club;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubRepository {
    List<Club> findByNameContaining(String query);
    List<Club> findPublicClubs(String query);
    Optional<Club> findByNameIgnoreCase(String name);
    Optional<Club> findById(UUID id);
    Optional<Club> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Club save(Club club);
}
