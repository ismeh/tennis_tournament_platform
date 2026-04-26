package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface JpaMemberRepository extends JpaRepository<MemberEntity, UUID> {

    Optional<MemberEntity> findByEmail(String username);

    @Modifying
    @Transactional
    @Query("UPDATE MemberEntity m SET m.tokenHash = :tokenHash WHERE m.id = :id")
    int updateTokenHash(@Param("id") UUID id, @Param("tokenHash") String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE MemberEntity m SET m.personId = :personId WHERE m.id = :id")
    int updatePersonId(@Param("id") UUID id, @Param("personId") UUID personId);

}
