package com.tfm.tennis_platform.domain.models.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Granular permissions available inside a club. Role defaults can be extended
 * per membership with custom grants.
 */
public enum ClubPermission {
    EDIT_CLUB_PAGE,
    VIEW_MEMBERS,
    MANAGE_MEMBERS,
    MANAGE_PERMISSIONS,
    VIEW_RANKINGS,
    MANAGE_TOURNAMENTS;

    public static Set<ClubPermission> all() {
        return Collections.unmodifiableSet(EnumSet.allOf(ClubPermission.class));
    }
}
