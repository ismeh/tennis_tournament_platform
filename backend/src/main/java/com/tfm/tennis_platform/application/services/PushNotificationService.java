package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.PushDeliveryResult;
import com.tfm.tennis_platform.domain.models.PushMessage;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PushNotificationSender;
import com.tfm.tennis_platform.domain.port.out.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registers browser push subscriptions and delivers push messages to a user's devices.
 *
 * Delivery is best-effort: a failing push service must never break the in-app notification,
 * so every error is logged and subscriptions rejected with HTTP 404/410 are removed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushNotificationSender pushNotificationSender;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public boolean isPushEnabled() {
        return pushNotificationSender.isEnabled();
    }

    @Transactional(readOnly = true)
    public String getPublicKey() {
        return pushNotificationSender.getPublicKey();
    }

    @Transactional
    public PushSubscription subscribe(
            String userEmail,
            String endpoint,
            String p256dh,
            String auth,
            String userAgent
    ) {
        if (isBlank(endpoint) || isBlank(p256dh) || isBlank(auth)) {
            throw new IllegalArgumentException("La suscripción push no es válida.");
        }

        Member member = getMemberByEmail(userEmail);
        LocalDateTime now = LocalDateTime.now();

        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(endpoint)
                .map(existing -> existing.toBuilder()
                        .userId(member.getId())
                        .p256dh(p256dh)
                        .auth(auth)
                        .userAgent(truncate(userAgent, 500))
                        .build())
                .orElseGet(() -> PushSubscription.builder()
                        .userId(member.getId())
                        .endpoint(endpoint)
                        .p256dh(p256dh)
                        .auth(auth)
                        .userAgent(truncate(userAgent, 500))
                        .createdAt(now)
                        .build());

        PushSubscription saved = pushSubscriptionRepository.save(subscription);
        log.info("Push subscription registered for user {} (endpoint host {})",
                member.getEmail(), hostOf(endpoint));
        return saved;
    }

    @Transactional
    public void unsubscribe(String userEmail, String endpoint) {
        if (isBlank(endpoint)) {
            return;
        }
        Member member = getMemberByEmail(userEmail);
        pushSubscriptionRepository.deleteByUserIdAndEndpoint(member.getId(), endpoint);
        log.info("Push subscription removed for user {}", member.getEmail());
    }

    @Transactional
    public int sendTestNotification(String userEmail) {
        Member member = getMemberByEmail(userEmail);
        return deliverToUser(
                member.getId(),
                "PuntoMatch",
                "Las notificaciones push están activadas correctamente.",
                "/ajustes",
                "push-test"
        );
    }

    @Transactional
    public int sendToUser(UUID userId, String title, String message, String linkUrl) {
        return deliverToUser(userId, title, message, linkUrl, null);
    }

    private int deliverToUser(UUID userId, String title, String message, String linkUrl, String tag) {
        if (!pushNotificationSender.isEnabled()) {
            return 0;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return 0;
        }

        PushMessage pushMessage = new PushMessage(title, message, linkUrl, null, tag);
        int delivered = 0;

        for (PushSubscription subscription : subscriptions) {
            PushDeliveryResult result = pushNotificationSender.send(subscription, pushMessage);
            switch (result.status()) {
                case DELIVERED -> {
                    delivered++;
                    pushSubscriptionRepository.updateLastUsedAt(subscription.getId(), LocalDateTime.now());
                }
                case EXPIRED -> {
                    log.info("Removing expired push subscription {} (HTTP {})",
                            subscription.getId(), result.statusCode());
                    pushSubscriptionRepository.deleteById(subscription.getId());
                }
                case FAILED -> log.warn("Push delivery failed for subscription {}: {}",
                        subscription.getId(), result.errorMessage());
                case SKIPPED -> {
                    return delivered;
                }
            }
        }

        return delivered;
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Member", email));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String hostOf(String endpoint) {
        try {
            return java.net.URI.create(endpoint).getHost();
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
