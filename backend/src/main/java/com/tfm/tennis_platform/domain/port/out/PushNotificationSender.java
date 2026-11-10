package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.PushDeliveryResult;
import com.tfm.tennis_platform.domain.models.PushMessage;
import com.tfm.tennis_platform.domain.models.PushSubscription;

/**
 * Sends a Web Push message to one subscription.
 *
 * Implementations must never throw on delivery errors: return a {@link PushDeliveryResult}
 * instead so notification delivery stays best-effort.
 */
public interface PushNotificationSender {

    boolean isEnabled();

    String getPublicKey();

    PushDeliveryResult send(PushSubscription subscription, PushMessage message);
}
