package com.tfm.tennis_platform.infrastructure.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.tennis_platform.domain.models.PushDeliveryResult;
import com.tfm.tennis_platform.domain.models.PushMessage;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.domain.port.out.PushNotificationSender;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Web Push sender backed by {@code dev.blanke.webpush} (VAPID + RFC 8291 encryption).
 *
 * The payload matches the shape consumed by the Angular service worker:
 * <pre>
 * { "notification": { "title", "body", "icon", "badge", "data": {
 *     "url", "onActionClick": { "default": { "operation", "url" } } } } }
 * </pre>
 * With {@code navigateLastFocusedOrOpen} a tap focuses the installed app and navigates to the
 * relevant route instead of opening a second window.
 */
@Component
@Slf4j
public class WebPushNotificationSender implements PushNotificationSender {

    private final PushProperties properties;
    private final ObjectMapper objectMapper;
    private final PushService pushService;
    private final String publicKey;

    public WebPushNotificationSender(PushProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        PushProperties.Vapid vapid = properties.getVapid();
        String configuredPublicKey = vapid != null ? trimToNull(vapid.getPublicKey()) : null;
        String configuredPrivateKey = vapid != null ? trimToNull(vapid.getPrivateKey()) : null;
        this.publicKey = configuredPublicKey;

        if (configuredPublicKey == null || configuredPrivateKey == null) {
            log.info("Web Push disabled: VAPID_PUBLIC_KEY/VAPID_PRIVATE_KEY are not configured");
            this.pushService = null;
            return;
        }

        PushService builtService = null;
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            builtService = PushService.builder()
                    .withVapidPublicKey(configuredPublicKey)
                    .withVapidPrivateKey(configuredPrivateKey)
                    .withVapidSubject(vapid.getSubject())
                    .build();
            log.info("Web Push enabled with VAPID subject {}", vapid.getSubject());
        } catch (Exception ex) {
            log.warn("Web Push disabled: invalid VAPID configuration ({})", ex.getMessage());
        }
        this.pushService = builtService;
    }

    @Override
    public boolean isEnabled() {
        return pushService != null;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public PushDeliveryResult send(PushSubscription subscription, PushMessage message) {
        if (pushService == null) {
            return PushDeliveryResult.skipped();
        }

        try {
            byte[] payload = objectMapper.writeValueAsBytes(buildPayload(message));
            Notification notification = Notification.builder()
                    .subscription(new Subscription(
                            subscription.getEndpoint(),
                            new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())))
                    .payload(payload)
                    .ttl(properties.getTtlSeconds())
                    .urgency(Notification.Urgency.NORMAL)
                    .build();

            HttpResponse<Void> response = pushService.send(notification);
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                return PushDeliveryResult.delivered();
            }
            if (status == 404 || status == 410) {
                return PushDeliveryResult.expired(status);
            }
            return PushDeliveryResult.failed(status, "Push service responded with HTTP " + status);
        } catch (Exception ex) {
            return PushDeliveryResult.failed(null, ex.getMessage());
        }
    }

    private Map<String, Object> buildPayload(PushMessage message) {
        String url = message.url() != null && !message.url().isBlank() ? message.url() : "/";

        Map<String, Object> defaultAction = new LinkedHashMap<>();
        defaultAction.put("operation", "navigateLastFocusedOrOpen");
        defaultAction.put("url", url);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);
        data.put("onActionClick", Map.of("default", defaultAction));

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", message.title());
        notification.put("body", message.body());
        String icon = message.icon() != null ? message.icon() : properties.getIcon();
        if (icon != null) {
            notification.put("icon", icon);
        }
        if (properties.getBadge() != null) {
            notification.put("badge", properties.getBadge());
        }
        if (message.tag() != null) {
            notification.put("tag", message.tag());
        }
        notification.put("data", data);

        return Map.of("notification", notification);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
