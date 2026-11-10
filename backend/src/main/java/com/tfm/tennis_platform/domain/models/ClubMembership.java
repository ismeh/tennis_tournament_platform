package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.ClubPermission;
import com.tfm.tennis_platform.domain.models.enums.ClubRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ClubMembership {
    private final UUID id;
    private final UUID clubId;
    private final UUID memberId;
    private final String email;
    private final UUID personId;
    private final ClubRole role;
    @Builder.Default
    private final Set<ClubPermission> customPermissions = EnumSet.noneOf(ClubPermission.class);
    private final LocalDateTime joinedAt;

    /**
     * Effective permissions include the role defaults plus any custom grants.
     */
    public Set<ClubPermission> effectivePermissions() {
        Set<ClubPermission> permissions = EnumSet.noneOf(ClubPermission.class);
        if (role != null) {
            permissions.addAll(role.defaultPermissions());
        }
        if (customPermissions != null) {
            permissions.addAll(customPermissions);
        }
        return Collections.unmodifiableSet(permissions);
    }

    public boolean hasPermission(ClubPermission permission) {
        return permission != null && effectivePermissions().contains(permission);
    }
}
