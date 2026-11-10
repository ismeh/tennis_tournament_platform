package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClubRepository extends JpaRepository<ClubEntity, UUID> {

    @Query("SELECT c FROM ClubEntity c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY c.name ASC")
    List<ClubEntity> findByNameContaining(@Param("query") String query);

    @Query("""
            SELECT c
            FROM ClubEntity c
            WHERE c.publicPageEnabled = true
              AND (
                  :query IS NULL
                  OR :query = ''
                  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(COALESCE(c.city, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY c.name ASC
            """)
    List<ClubEntity> findPublicClubs(@Param("query") String query);

    Optional<ClubEntity> findByNameIgnoreCase(String name);

    Optional<ClubEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
