package com.tfm.tennis_platform.infrastructure.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.tennis_platform.domain.models.PushDeliveryResult;
import com.tfm.tennis_platform.domain.models.PushMessage;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WebPushNotificationSenderTest {

    private PushSubscription subscription() {
        return PushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .endpoint("https://push.example/endpoint")
                .p256dh("p256dh")
                .auth("auth")
                .build();
    }

    @Test
    void should_stay_disabled_when_vapid_keys_are_missing() {
        PushProperties properties = new PushProperties();

        WebPushNotificationSender sender = new WebPushNotificationSender(properties, new ObjectMapper());

        assertFalse(sender.isEnabled());
        PushDeliveryResult result = sender.send(
                subscription(),
                new PushMessage("Título", "Mensaje", "/torneos/1", null, null)
        );
        assertEquals(PushDeliveryResult.Status.SKIPPED, result.status());
    }

    @Test
    void should_stay_disabled_when_the_key_pair_is_invalid() {
        PushProperties properties = new PushProperties();
        properties.getVapid().setPublicKey("not-a-key");
        properties.getVapid().setPrivateKey("not-a-key");

        WebPushNotificationSender sender = new WebPushNotificationSender(properties, new ObjectMapper());

        assertFalse(sender.isEnabled());
    }

    @Test
    void should_expose_the_configured_public_key() {
        PushProperties properties = new PushProperties();
        properties.getVapid().setPublicKey("public-key");

        WebPushNotificationSender sender = new WebPushNotificationSender(properties, new ObjectMapper());

        assertEquals("public-key", sender.getPublicKey());
        assertFalse(sender.isEnabled());
    }

    @Test
    void should_be_enabled_with_a_valid_vapid_key_pair() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
        KeyPair keyPair = generator.generateKeyPair();

        var publicKey = (org.bouncycastle.jce.interfaces.ECPublicKey) keyPair.getPublic();
        var privateKey = (org.bouncycastle.jce.interfaces.ECPrivateKey) keyPair.getPrivate();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        PushProperties properties = new PushProperties();
        properties.getVapid().setPublicKey(encoder.encodeToString(publicKey.getQ().getEncoded(false)));
        properties.getVapid().setPrivateKey(
                encoder.encodeToString(BigIntegers.asUnsignedByteArray(32, privateKey.getD())));

        WebPushNotificationSender sender = new WebPushNotificationSender(properties, new ObjectMapper());

        assertTrue(sender.isEnabled());
        assertEquals(properties.getVapid().getPublicKey(), sender.getPublicKey());
    }
}
