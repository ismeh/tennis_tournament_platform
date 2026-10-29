package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.AgeCategoryRef;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgeCategoryRefRepository {
    List<AgeCategoryRef> findAll();
    List<AgeCategoryRef> findByOrganizerId(UUID organizerId);
    Optional<AgeCategoryRef> findById(Integer id);
    AgeCategoryRef save(AgeCategoryRef category);
    void deleteById(Integer id);
    boolean existsByOrganizerIdAndCategory(UUID organizerId, String category);
}
