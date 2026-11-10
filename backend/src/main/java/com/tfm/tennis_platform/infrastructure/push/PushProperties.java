package com.tfm.tennis_platform.infrastructure.push;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for Web Push delivery.
 *
 * When the VAPID key pair is missing push stays disabled: the app boots normally and the
 * frontend receives {@code enabled=false} from {@code GET /api/push/public-key}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.push")
public class PushProperties {

    private Vapid vapid = new Vapid();

    /** How long the push service keeps the message if the device is offline. */
    private int ttlSeconds = 86_400;

    private String icon = "/icons/icon-192.png";

    private String badge = "/icons/favicon-32.png";

    @Getter
    @Setter
    public static class Vapid {
        /** URL-safe base64 encoded, uncompressed P-256 public key. */
        private String publicKey;
        /** URL-safe base64 encoded P-256 private key. */
        private String privateKey;
        /** Contact URI, typically a mailto: address. */
        private String subject = "mailto:no-reply@puntomatch.local";
    }
}
