package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ClubMembership;
import com.tfm.tennis_platform.domain.port.out.ClubMembershipRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubMembershipEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ClubMembershipMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaClubMembershipRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClubMembershipRepositoryAdapter implements ClubMembershipRepository {

    private final JpaClubMembershipRepository membershipRepository;
    private final ClubMembershipMapper mapper;
    private final EntityManager entityManager;

    @Override
    public ClubMembership save(ClubMembership membership) {
        ClubMembershipEntity entity = membership.getId() != null
                ? membershipRepository.findById(membership.getId()).orElseGet(ClubMembershipEntity::new)
                : new ClubMembershipEntity();

        entity.setClub(entityManager.getReference(ClubEntity.class, membership.getClubId()));
        entity.setMember(entityManager.getReference(MemberEntity.class, membership.getMemberId()));
        entity.setRole(membership.getRole());
        entity.setCustomPermissions(membership.getCustomPermissions() != null
                ? new HashSet<>(membership.getCustomPermissions())
                : new HashSet<>());

        return mapper.toDomain(membershipRepository.save(entity));
    }

    @Override
    public Optional<ClubMembership> findById(UUID id) {
        return membershipRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ClubMembership> findByClubIdAndMemberId(UUID clubId, UUID memberId) {
        return membershipRepository.findByClubIdAndMemberId(clubId, memberId).map(mapper::toDomain);
    }

    @Override
    public List<ClubMembership> findByClubId(UUID clubId) {
        return membershipRepository.findByClubId(clubId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ClubMembership> findByMemberId(UUID memberId) {
        return membershipRepository.findByMemberId(memberId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByClubIdAndMemberId(UUID clubId, UUID memberId) {
        return membershipRepository.existsByClubIdAndMemberId(clubId, memberId);
    }

    @Override
    public long countByClubId(UUID clubId) {
        return membershipRepository.countByClubId(clubId);
    }

    @Override
    public void delete(UUID id) {
        membershipRepository.deleteById(id);
    }
}
