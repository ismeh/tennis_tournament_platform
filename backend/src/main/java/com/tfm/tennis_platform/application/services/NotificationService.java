package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Notification;
import com.tfm.tennis_platform.domain.models.enums.NotificationChannel;
import com.tfm.tennis_platform.domain.models.enums.NotificationType;
import com.tfm.tennis_platform.domain.port.out.EmailSender;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.NotificationRepository;
import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ParticipantEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.PersonEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final EmailSender emailSender;
    private final JpaInscriptionRepository jpaInscriptionRepository;
    private final JpaMemberRepository jpaMemberRepository;

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String userEmail) {
        Member member = getMemberByEmail(userEmail);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(member.getId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        Member member = getMemberByEmail(userEmail);
        return notificationRepository.countUnreadByUserId(member.getId());
    }

    @Transactional
    public void markAsRead(UUID notificationId, String userEmail) {
        Member member = getMemberByEmail(userEmail);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(member.getId())) {
            throw new IllegalArgumentException("No puedes modificar notificaciones de otro usuario.");
        }

        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        Member member = getMemberByEmail(userEmail);
        notificationRepository.markAllAsRead(member.getId());
    }

    @Transactional(readOnly = true)
    public NotificationChannel getNotificationChannel(String userEmail) {
        Member member = getMemberByEmail(userEmail);
        return member.getNotificationChannel() != null ? member.getNotificationChannel() : NotificationChannel.BOTH;
    }

    @Transactional
    public void updateNotificationChannel(String userEmail, NotificationChannel channel) {
        Member member = getMemberByEmail(userEmail);
        NotificationChannel resolvedChannel = channel != null ? channel : NotificationChannel.BOTH;
        memberRepository.updateNotificationChannel(member.getId(), resolvedChannel);
        log.info("Notification channel preference updated for user {} to {}", userEmail, resolvedChannel);
    }

    @Transactional
    public void sendNotification(UUID userId, String title, String message, NotificationType type, String linkUrl) {
        Member member = memberRepository.findById(userId).orElse(null);
        if (member == null) {
            log.warn("Cannot send notification: User {} not found", userId);
            return;
        }

        NotificationChannel channel = member.getNotificationChannel() != null
                ? member.getNotificationChannel()
                : NotificationChannel.BOTH;

        if (channel == NotificationChannel.NONE) {
            log.debug("User {} has notifications disabled (NONE)", member.getEmail());
            return;
        }

        if (channel.includesApp()) {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(type != null ? type : NotificationType.GENERAL)
                    .linkUrl(linkUrl)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }

        if (channel.includesEmail()) {
            try {
                emailSender.sendNotificationEmail(member.getEmail(), title, message);
            } catch (Exception ex) {
                log.warn("Failed to deliver notification email to {}: {}", member.getEmail(), ex.getMessage());
            }
        }
    }

    @Transactional
    public void notifyMatchScheduled(UUID tournamentId, String tournamentName, UUID firstInscriptionId, UUID secondInscriptionId, LocalDateTime scheduledAt, String courtName) {
        String formattedDate = scheduledAt != null
                ? scheduledAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "por determinar";
        String courtInfo = courtName != null && !courtName.isBlank() ? " en " + courtName : "";

        String title = "Partido programado en " + (tournamentName != null ? tournamentName : "Torneo");
        String message = String.format("Tu próximo partido ha sido programado para el %s%s.", formattedDate, courtInfo);
        String linkUrl = tournamentId != null ? "/torneos/" + tournamentId : "/calendario";

        List<UUID> recipientUserIds = new ArrayList<>();
        findUserIdsForInscription(firstInscriptionId, recipientUserIds);
        findUserIdsForInscription(secondInscriptionId, recipientUserIds);

        for (UUID userId : recipientUserIds.stream().distinct().toList()) {
            sendNotification(userId, title, message, NotificationType.MATCH_SCHEDULED, linkUrl);
        }
    }

    private void findUserIdsForInscription(UUID inscriptionId, List<UUID> recipientUserIds) {
        if (inscriptionId == null) {
            return;
        }
        jpaInscriptionRepository.findDetailedById(inscriptionId).ifPresent(entity -> {
            ParticipantEntity participant = entity.getParticipant();
            if (participant == null) {
                return;
            }
            if (participant.getIndividualPerson() != null) {
                findUserForPerson(participant.getIndividualPerson().getId(), recipientUserIds);
            }
            if (participant.getMembers() != null) {
                for (PersonEntity person : participant.getMembers()) {
                    if (person != null && person.getId() != null) {
                        findUserForPerson(person.getId(), recipientUserIds);
                    }
                }
            }
        });
    }

    private void findUserForPerson(UUID personId, List<UUID> recipientUserIds) {
        jpaMemberRepository.findByPersonId(personId).ifPresent(member -> recipientUserIds.add(member.getId()));
    }

    private Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Member", email));
    }
}
