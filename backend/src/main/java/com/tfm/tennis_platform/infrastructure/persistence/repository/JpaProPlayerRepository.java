package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaProPlayerRepository extends JpaRepository<ProPlayerEntity, Integer> {

    List<ProPlayerEntity> findTop10ByOrderByRankingPositionAsc();

    Optional<ProPlayerEntity> findFirstByLicenseIgnoreCase(String license);

    @Query("""
            SELECT p
            FROM ProPlayerEntity p
            WHERE LOWER(p.license) IN :licenses
            """)
    List<ProPlayerEntity> findByNormalizedLicenses(@Param("licenses") List<String> licenses);

    @Query("""
            SELECT p
            FROM ProPlayerEntity p
            WHERE :query IS NULL
               OR :query = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(p.license, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(p.clubName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY p.rankingPosition ASC
            """)
    List<ProPlayerEntity> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query("""
            SELECT p
            FROM ProPlayerEntity p
            WHERE (:gender IS NULL OR UPPER(COALESCE(p.gender, '')) = :gender)
              AND (:category IS NULL OR UPPER(COALESCE(p.ageCategory, '')) = :category)
            """)
    Page<ProPlayerEntity> findRanking(
            @Param("gender") String gender,
            @Param("category") String category,
            Pageable pageable
    );
}
