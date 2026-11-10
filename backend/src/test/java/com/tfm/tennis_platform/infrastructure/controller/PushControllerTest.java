package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.PushNotificationService;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushPublicKeyResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushSubscriptionKeys;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushSubscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushTestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushControllerTest {

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private PushController controller;

    private final Principal principal = () -> "player@test.com";

    @Test
    void should_expose_the_public_key() {
        when(pushNotificationService.getPublicKey()).thenReturn("public-key");
        when(pushNotificationService.isPushEnabled()).thenReturn(true);

        ResponseEntity<PushPublicKeyResponse> result = controller.getPublicKey();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().publicKey()).isEqualTo("public-key");
        assertThat(result.getBody().enabled()).isTrue();
    }

    @Test
    void should_register_a_subscription() {
        UUID id = UUID.randomUUID();
        PushSubscription saved = PushSubscription.builder()
                .id(id)
                .endpoint("https://push.example/endpoint")
                .build();
        when(pushNotificationService.subscribe(
                eq("player@test.com"),
                eq("https://push.example/endpoint"),
                eq("p256dh"),
                eq("auth"),
                eq("Chrome")))
                .thenReturn(saved);

        ResponseEntity<Void> result = controller.subscribe(
                new PushSubscriptionRequest(
                        "https://push.example/endpoint",
                        new PushSubscriptionKeys("p256dh", "auth"),
                        "Chrome"),
                principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void should_unsubscribe_an_endpoint() {
        ResponseEntity<Void> result = controller.unsubscribe("https://push.example/endpoint", principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(pushNotificationService).unsubscribe("player@test.com", "https://push.example/endpoint");
    }

    @Test
    void should_send_a_test_notification() {
        when(pushNotificationService.sendTestNotification("player@test.com")).thenReturn(1);

        ResponseEntity<PushTestResponse> result = controller.sendTestNotification(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().delivered()).isEqualTo(1);
    }
}
