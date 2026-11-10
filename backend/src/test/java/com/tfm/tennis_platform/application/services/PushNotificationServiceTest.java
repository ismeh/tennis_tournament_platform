package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.PushDeliveryResult;
import com.tfm.tennis_platform.domain.models.PushSubscription;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PushNotificationSender;
import com.tfm.tennis_platform.domain.port.out.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private PushNotificationSender pushNotificationSender;

    @Mock
    private MemberRepository memberRepository;

    private PushNotificationService pushNotificationService;

    private final String email = "player@example.com";
    private final UUID userId = UUID.randomUUID();
    private final String endpoint = "https://fcm.googleapis.com/fcm/send/abc";

    @BeforeEach
    void setUp() {
        pushNotificationService = new PushNotificationService(
                pushSubscriptionRepository,
                pushNotificationSender,
                memberRepository
        );
    }

    @Test
    void should_register_a_new_subscription() {
        Member member = Member.builder().id(userId).email(email).build();
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        when(pushSubscriptionRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());
        when(pushSubscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PushSubscription result = pushNotificationService.subscribe(email, endpoint, "p256dh", "auth", "Firefox");

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(endpoint, result.getEndpoint());
        assertEquals("p256dh", result.getP256dh());
        assertEquals("Firefox", result.getUserAgent());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void should_reassign_an_existing_subscription_to_the_current_user() {
        UUID previousUserId = UUID.randomUUID();
        Member member = Member.builder().id(userId).email(email).build();
        PushSubscription existing = PushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(previousUserId)
                .endpoint(endpoint)
                .p256dh("old")
                .auth("old")
                .build();

        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        when(pushSubscriptionRepository.findByEndpoint(endpoint)).thenReturn(Optional.of(existing));
        when(pushSubscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PushSubscription result = pushNotificationService.subscribe(email, endpoint, "new", "new-auth", null);

        assertEquals(userId, result.getUserId());
        assertEquals("new", result.getP256dh());
    }

    @Test
    void should_reject_an_incomplete_subscription() {
        assertThrows(IllegalArgumentException.class,
                () -> pushNotificationService.subscribe(email, endpoint, "", "auth", null));
    }

    @Test
    void should_unsubscribe_only_the_current_users_endpoint() {
        Member member = Member.builder().id(userId).email(email).build();
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));

        pushNotificationService.unsubscribe(email, endpoint);

        verify(pushSubscriptionRepository).deleteByUserIdAndEndpoint(userId, endpoint);
    }

    @Test
    void should_throw_when_user_does_not_exist() {
        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pushNotificationService.sendTestNotification(email));
    }

    @Test
    void should_deliver_to_every_subscription_and_update_last_used() {
        PushSubscription subscription = PushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .endpoint(endpoint)
                .p256dh("p256dh")
                .auth("auth")
                .build();

        when(pushNotificationSender.isEnabled()).thenReturn(true);
        when(pushSubscriptionRepository.findByUserId(userId)).thenReturn(List.of(subscription));
        when(pushNotificationSender.send(eq(subscription), any())).thenReturn(PushDeliveryResult.delivered());

        int delivered = pushNotificationService.sendToUser(userId, "Título", "Mensaje", "/torneos/1");

        assertEquals(1, delivered);
        verify(pushSubscriptionRepository).updateLastUsedAt(eq(subscription.getId()), any());
        verify(pushSubscriptionRepository, never()).deleteById(any());
    }

    @Test
    void should_remove_expired_subscriptions() {
        PushSubscription subscription = PushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .endpoint(endpoint)
                .p256dh("p256dh")
                .auth("auth")
                .build();

        when(pushNotificationSender.isEnabled()).thenReturn(true);
        when(pushSubscriptionRepository.findByUserId(userId)).thenReturn(List.of(subscription));
        when(pushNotificationSender.send(eq(subscription), any())).thenReturn(PushDeliveryResult.expired(410));

        int delivered = pushNotificationService.sendToUser(userId, "Título", "Mensaje", "/torneos/1");

        assertEquals(0, delivered);
        verify(pushSubscriptionRepository).deleteById(subscription.getId());
    }

    @Test
    void should_not_send_when_push_is_disabled() {
        when(pushNotificationSender.isEnabled()).thenReturn(false);

        int delivered = pushNotificationService.sendToUser(userId, "Título", "Mensaje", "/torneos/1");

        assertEquals(0, delivered);
        verify(pushSubscriptionRepository, never()).findByUserId(any());
    }

    @Test
    void should_send_a_test_notification_to_the_current_user() {
        Member member = Member.builder().id(userId).email(email).build();
        PushSubscription subscription = PushSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .endpoint(endpoint)
                .p256dh("p256dh")
                .auth("auth")
                .build();

        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        when(pushNotificationSender.isEnabled()).thenReturn(true);
        when(pushSubscriptionRepository.findByUserId(userId)).thenReturn(List.of(subscription));
        when(pushNotificationSender.send(eq(subscription), any())).thenReturn(PushDeliveryResult.delivered());

        int delivered = pushNotificationService.sendTestNotification(email);

        assertEquals(1, delivered);
        ArgumentCaptor<com.tfm.tennis_platform.domain.models.PushMessage> captor =
                ArgumentCaptor.forClass(com.tfm.tennis_platform.domain.models.PushMessage.class);
        verify(pushNotificationSender).send(eq(subscription), captor.capture());
        assertEquals("/ajustes", captor.getValue().url());
    }
}
