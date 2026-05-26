package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaPersonRepository extends JpaRepository<PersonEntity, UUID> {
    PersonEntity saveAndFlush(PersonEntity entity);

    List<PersonEntity> findTop20ByOrderByFirstNameAscLastNameAsc();

    @Query("""
            SELECT p
            FROM PersonEntity p
            WHERE :query IS NULL
               OR :query = ''
               OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(p.tennisId, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY p.firstName ASC, p.lastName ASC
            """)
    List<PersonEntity> searchByQuery(String query);
}
