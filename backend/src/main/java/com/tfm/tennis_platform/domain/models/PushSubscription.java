package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A browser push subscription owned by a user.
 *
 * The endpoint identifies the browser/push service pair; p256dh and auth are the keys
 * the Web Push encryption needs. A user can keep several subscriptions (phone, laptop...).
 */
@Getter
@Builder(toBuilder = true)
public class PushSubscription {
    private final UUID id;
    private final UUID userId;
    private final String endpoint;
    private final String p256dh;
    private final String auth;
    private final String userAgent;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUsedAt;
}
