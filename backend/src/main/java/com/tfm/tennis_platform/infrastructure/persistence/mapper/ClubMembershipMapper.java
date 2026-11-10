package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.ClubMembership;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubMembershipEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ClubEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class ClubMembershipMapper {

    public ClubMembership toDomain(ClubMembershipEntity entity) {
        if (entity == null) {
            return null;
        }
        MemberEntity member = entity.getMember();
        return ClubMembership.builder()
                .id(entity.getId())
                .clubId(entity.getClub() != null ? entity.getClub().getId() : null)
                .memberId(member != null ? member.getId() : null)
                .email(member != null ? member.getEmail() : null)
                .personId(member != null ? member.getPersonId() : null)
                .role(entity.getRole())
                .customPermissions(entity.getCustomPermissions() != null
                        ? new HashSet<>(entity.getCustomPermissions())
                        : new HashSet<>())
                .joinedAt(entity.getJoinedAt())
                .build();
    }

    public ClubMembershipEntity toEntity(ClubMembership domain) {
        if (domain == null) {
            return null;
        }
        return ClubMembershipEntity.builder()
                .id(domain.getId())
                .club(ClubEntity.builder().id(domain.getClubId()).build())
                .member(MemberEntity.builder().id(domain.getMemberId()).build())
                .role(domain.getRole())
                .customPermissions(domain.getCustomPermissions() != null
                        ? new HashSet<>(domain.getCustomPermissions())
                        : new HashSet<>())
                .build();
    }
}
