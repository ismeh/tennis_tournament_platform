package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.PushNotificationService;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushPublicKeyResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushSubscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.PushTestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushNotificationService pushNotificationService;

    /**
     * Public: the VAPID public key is public information. The {@code enabled} flag lets the UI
     * explain when push is not configured for this environment.
     */
    @GetMapping("/public-key")
    public ResponseEntity<PushPublicKeyResponse> getPublicKey() {
        return ResponseEntity.ok(new PushPublicKeyResponse(
                pushNotificationService.getPublicKey(),
                pushNotificationService.isPushEnabled()
        ));
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(
            @RequestBody PushSubscriptionRequest request,
            Principal principal
    ) {
        if (request == null || request.keys() == null) {
            throw new IllegalArgumentException("La suscripción push no es válida.");
        }

        PushSubscription subscription = pushNotificationService.subscribe(
                principal.getName(),
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth(),
                request.userAgent()
        );

        return ResponseEntity.created(java.net.URI.create("/api/push/subscriptions/" + subscription.getId()))
                .build();
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint, Principal principal) {
        pushNotificationService.unsubscribe(principal.getName(), endpoint);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<PushTestResponse> sendTestNotification(Principal principal) {
        int delivered = pushNotificationService.sendTestNotification(principal.getName());
        return ResponseEntity.ok(new PushTestResponse(delivered));
    }
}
