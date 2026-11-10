package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClubMembershipRepository extends JpaRepository<ClubMembershipEntity, UUID> {

    @Query("""
            SELECT m
            FROM ClubMembershipEntity m
            JOIN FETCH m.member
            WHERE m.club.id = :clubId
            ORDER BY m.role ASC, m.joinedAt ASC
            """)
    List<ClubMembershipEntity> findByClubId(@Param("clubId") UUID clubId);

    @Query("""
            SELECT m
            FROM ClubMembershipEntity m
            JOIN FETCH m.club
            JOIN FETCH m.member
            WHERE m.member.id = :memberId
            ORDER BY m.joinedAt ASC
            """)
    List<ClubMembershipEntity> findByMemberId(@Param("memberId") UUID memberId);

    @Query("""
            SELECT m
            FROM ClubMembershipEntity m
            JOIN FETCH m.member
            WHERE m.club.id = :clubId AND m.member.id = :memberId
            """)
    Optional<ClubMembershipEntity> findByClubIdAndMemberId(
            @Param("clubId") UUID clubId,
            @Param("memberId") UUID memberId
    );

    boolean existsByClubIdAndMemberId(UUID clubId, UUID memberId);

    long countByClubId(UUID clubId);
}
