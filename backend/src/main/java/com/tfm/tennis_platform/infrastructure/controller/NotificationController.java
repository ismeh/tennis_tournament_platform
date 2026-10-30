package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.NotificationService;
import com.tfm.tennis_platform.domain.models.Notification;
import com.tfm.tennis_platform.domain.models.enums.NotificationChannel;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationChannelPreferenceRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationChannelPreferenceResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.NotificationUnreadCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.NotificationWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationWebMapper notificationWebMapper;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(Principal principal) {
        List<Notification> notifications = notificationService.getUserNotifications(principal.getName());
        return ResponseEntity.ok(notifications.stream().map(notificationWebMapper::toResponse).toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount(Principal principal) {
        long count = notificationService.getUnreadCount(principal.getName());
        return ResponseEntity.ok(new NotificationUnreadCountResponse(count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, Principal principal) {
        notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationChannelPreferenceResponse> getPreference(Principal principal) {
        NotificationChannel channel = notificationService.getNotificationChannel(principal.getName());
        return ResponseEntity.ok(new NotificationChannelPreferenceResponse(channel));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationChannelPreferenceResponse> updatePreference(
            @RequestBody NotificationChannelPreferenceRequest request,
            Principal principal
    ) {
        notificationService.updateNotificationChannel(principal.getName(), request.channel());
        return ResponseEntity.ok(new NotificationChannelPreferenceResponse(request.channel()));
    }
}
