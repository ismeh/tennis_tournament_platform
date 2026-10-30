package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.NotificationService;
import com.tfm.tennis_platform.domain.models.Notification;
import com.tfm.tennis_platform.domain.models.enums.NotificationChannel;
import com.tfm.tennis_platform.domain.models.enums.NotificationType;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationChannelPreferenceRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationChannelPreferenceResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationUnreadCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.NotificationWebMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationWebMapper notificationWebMapper;

    @InjectMocks
    private NotificationController controller;

    private final Principal principal = () -> "player@test.com";

    @Test
    void should_get_user_notifications() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .title("Aviso")
                .message("Texto")
                .type(NotificationType.GENERAL)
                .build();
        NotificationResponse response = new NotificationResponse(
                notification.getId(),
                "Aviso",
                "Texto",
                NotificationType.GENERAL,
                null,
                false,
                LocalDateTime.now()
        );

        when(notificationService.getUserNotifications("player@test.com")).thenReturn(List.of(notification));
        when(notificationWebMapper.toResponse(any())).thenReturn(response);

        ResponseEntity<List<NotificationResponse>> result = controller.getUserNotifications(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(notificationService).getUserNotifications("player@test.com");
    }

    @Test
    void should_get_unread_count() {
        when(notificationService.getUnreadCount("player@test.com")).thenReturn(3L);

        ResponseEntity<NotificationUnreadCountResponse> result = controller.getUnreadCount(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().unreadCount()).isEqualTo(3L);
        verify(notificationService).getUnreadCount("player@test.com");
    }

    @Test
    void should_mark_as_read() {
        UUID notifId = UUID.randomUUID();

        ResponseEntity<Void> result = controller.markAsRead(notifId, principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationService).markAsRead(notifId, "player@test.com");
    }

    @Test
    void should_mark_all_as_read() {
        ResponseEntity<Void> result = controller.markAllAsRead(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationService).markAllAsRead("player@test.com");
    }

    @Test
    void should_get_channel_preference() {
        when(notificationService.getNotificationChannel("player@test.com")).thenReturn(NotificationChannel.EMAIL);

        ResponseEntity<NotificationChannelPreferenceResponse> result = controller.getPreference(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().channel()).isEqualTo(NotificationChannel.EMAIL);
        verify(notificationService).getNotificationChannel("player@test.com");
    }

    @Test
    void should_update_channel_preference() {
        NotificationChannelPreferenceRequest request = new NotificationChannelPreferenceRequest(NotificationChannel.APP);

        ResponseEntity<NotificationChannelPreferenceResponse> result = controller.updatePreference(request, principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().channel()).isEqualTo(NotificationChannel.APP);
        verify(notificationService).updateNotificationChannel("player@test.com", NotificationChannel.APP);
    }
}
