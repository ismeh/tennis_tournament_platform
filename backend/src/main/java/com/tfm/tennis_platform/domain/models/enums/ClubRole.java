package com.tfm.tennis_platform.domain.models.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Organizational role of a member inside a club.
 */
public enum ClubRole {
    OWNER,
    ADMIN,
    COACH,
    MEMBER;

    public Set<ClubPermission> defaultPermissions() {
        return switch (this) {
            case OWNER -> ClubPermission.all();
            case ADMIN -> Collections.unmodifiableSet(EnumSet.of(
                    ClubPermission.EDIT_CLUB_PAGE,
                    ClubPermission.VIEW_MEMBERS,
                    ClubPermission.MANAGE_MEMBERS,
                    ClubPermission.VIEW_RANKINGS,
                    ClubPermission.MANAGE_TOURNAMENTS
            ));
            case COACH -> Collections.unmodifiableSet(EnumSet.of(
                    ClubPermission.VIEW_MEMBERS,
                    ClubPermission.VIEW_RANKINGS
            ));
            case MEMBER -> Collections.unmodifiableSet(EnumSet.of(
                    ClubPermission.VIEW_RANKINGS
            ));
        };
    }
}
