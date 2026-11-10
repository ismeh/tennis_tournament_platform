package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.ClubMembership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubMembershipRepository {
    ClubMembership save(ClubMembership membership);
    Optional<ClubMembership> findById(UUID id);
    Optional<ClubMembership> findByClubIdAndMemberId(UUID clubId, UUID memberId);
    List<ClubMembership> findByClubId(UUID clubId);
    List<ClubMembership> findByMemberId(UUID memberId);
    boolean existsByClubIdAndMemberId(UUID clubId, UUID memberId);
    long countByClubId(UUID clubId);
    void delete(UUID id);
}
