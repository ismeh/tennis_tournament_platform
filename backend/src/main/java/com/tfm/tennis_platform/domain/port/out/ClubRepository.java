package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Club;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubRepository {
    List<Club> findByNameContaining(String query);
    Optional<Club> findByNameIgnoreCase(String name);
    Club save(Club club);
}
