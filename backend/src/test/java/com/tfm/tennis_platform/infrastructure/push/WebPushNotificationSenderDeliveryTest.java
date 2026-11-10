package com.tfm.tennis_platform.infrastructure.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tfm.tennis_platform.domain.models.PushDeliveryResult;
import com.tfm.tennis_platform.domain.models.PushMessage;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the full sender path (VAPID JWT + payload encryption + HTTP POST) against a local
 * stub push service. No external network is required.
 */
class WebPushNotificationSenderDeliveryTest {

    private HttpServer server;
    private final AtomicReference<byte[]> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();
    private final AtomicReference<String> receivedContentEncoding = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/push", exchange -> {
            receivedBody.set(exchange.getRequestBody().readAllBytes());
            receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            receivedContentEncoding.set(exchange.getRequestHeaders().getFirst("Content-Encoding"));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void should_encrypt_and_deliver_a_push_message() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        KeyPair vapidKeyPair = generateKeyPair();
        KeyPair clientKeyPair = generateKeyPair();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        PushProperties properties = new PushProperties();
        properties.getVapid().setPublicKey(
                encoder.encodeToString(((org.bouncycastle.jce.interfaces.ECPublicKey) vapidKeyPair.getPublic())
                        .getQ().getEncoded(false)));
        properties.getVapid().setPrivateKey(encoder.encodeToString(
                BigIntegers.asUnsignedByteArray(
                        32, ((org.bouncycastle.jce.interfaces.ECPrivateKey) vapidKeyPair.getPrivate()).getD())));

        byte[] authSecret = new byte[16];
        new SecureRandom().nextBytes(authSecret);

        PushSubscription subscription = PushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .endpoint("http://localhost:" + server.getAddress().getPort() + "/push/endpoint")
                .p256dh(encoder.encodeToString(
                        ((org.bouncycastle.jce.interfaces.ECPublicKey) clientKeyPair.getPublic())
                                .getQ().getEncoded(false)))
                .auth(encoder.encodeToString(authSecret))
                .build();

        WebPushNotificationSender sender = new WebPushNotificationSender(properties, new ObjectMapper());
        PushDeliveryResult result = sender.send(
                subscription,
                new PushMessage("Partido programado", "Pista Central a las 10:00", "/torneos/1", null, null));

        assertEquals(PushDeliveryResult.Status.DELIVERED, result.status());
        assertNotNull(receivedBody.get());
        assertTrue(receivedBody.get().length > 0, "Encrypted payload should not be empty");
        assertTrue(receivedAuthorization.get().startsWith("vapid t="), "Request should use VAPID");
        assertEquals("aes128gcm", receivedContentEncoding.get());
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
        return generator.generateKeyPair();
    }
}
