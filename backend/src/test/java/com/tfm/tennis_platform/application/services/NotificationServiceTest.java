package com.tfm.tennis_platform.application.services;

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
import com.tfm.tennis_platform.infrastructure.persistence.entity.MemberEntity;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaInscriptionRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private JpaInscriptionRepository jpaInscriptionRepository;

    @Mock
    private JpaMemberRepository jpaMemberRepository;

    private NotificationService notificationService;

    private final String testEmail = "test@example.com";
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                memberRepository,
                emailSender,
                jpaInscriptionRepository,
                jpaMemberRepository
        );
    }

    @Test
    void should_return_user_notifications() {
        Member member = Member.builder().id(userId).email(testEmail).build();
        Notification n = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title("Aviso")
                .message("Mensaje de prueba")
                .type(NotificationType.GENERAL)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(n));

        List<Notification> result = notificationService.getUserNotifications(testEmail);

        assertEquals(1, result.size());
        assertEquals("Aviso", result.get(0).getTitle());
    }

    @Test
    void should_return_unread_count() {
        Member member = Member.builder().id(userId).email(testEmail).build();
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(testEmail);

        assertEquals(5L, count);
    }

    @Test
    void should_mark_as_read_when_notification_belongs_to_user() {
        Member member = Member.builder().id(userId).email(testEmail).build();
        UUID notifId = UUID.randomUUID();
        Notification n = Notification.builder()
                .id(notifId)
                .userId(userId)
                .title("Test")
                .message("Test")
                .type(NotificationType.GENERAL)
                .read(false)
                .build();

        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        notificationService.markAsRead(notifId, testEmail);

        verify(notificationRepository).markAsRead(notifId);
    }

    @Test
    void should_throw_when_marking_as_read_notification_of_another_user() {
        Member member = Member.builder().id(userId).email(testEmail).build();
        UUID otherUserId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        Notification n = Notification.builder()
                .id(notifId)
                .userId(otherUserId)
                .title("Test")
                .message("Test")
                .type(NotificationType.GENERAL)
                .read(false)
                .build();

        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        assertThrows(IllegalArgumentException.class, () -> notificationService.markAsRead(notifId, testEmail));
        verify(notificationRepository, never()).markAsRead(notifId);
    }

    @Test
    void should_mark_all_as_read() {
        Member member = Member.builder().id(userId).email(testEmail).build();
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));

        notificationService.markAllAsRead(testEmail);

        verify(notificationRepository).markAllAsRead(userId);
    }

    @Test
    void should_return_user_notification_channel() {
        Member member = Member.builder()
                .id(userId)
                .email(testEmail)
                .notificationChannel(NotificationChannel.EMAIL)
                .build();
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));

        NotificationChannel channel = notificationService.getNotificationChannel(testEmail);

        assertEquals(NotificationChannel.EMAIL, channel);
    }

    @Test
    void should_update_notification_channel() {
        Member member = Member.builder().id(userId).email(testEmail).build();
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(member));

        notificationService.updateNotificationChannel(testEmail, NotificationChannel.APP);

        verify(memberRepository).updateNotificationChannel(userId, NotificationChannel.APP);
    }

    @Test
    void should_send_app_and_email_notification_when_channel_is_both() {
        Member member = Member.builder()
                .id(userId)
                .email("test@example.com")
                .notificationChannel(NotificationChannel.BOTH)
                .build();

        when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

        notificationService.sendNotification(
                userId,
                "Partido programado",
                "Tu partido es hoy",
                NotificationType.MATCH_SCHEDULED,
                "/tournaments/1"
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(emailSender).sendNotificationEmail(eq("test@example.com"), eq("Partido programado"), eq("Tu partido es hoy"));
    }

    @Test
    void should_only_send_email_when_channel_is_email() {
        Member member = Member.builder()
                .id(userId)
                .email("test@example.com")
                .notificationChannel(NotificationChannel.EMAIL)
                .build();

        when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

        notificationService.sendNotification(
                userId,
                "Aviso",
                "Mensaje",
                NotificationType.GENERAL,
                null
        );

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender).sendNotificationEmail(eq("test@example.com"), eq("Aviso"), eq("Mensaje"));
    }

    @Test
    void should_only_save_app_notification_when_channel_is_app() {
        Member member = Member.builder()
                .id(userId)
                .email("test@example.com")
                .notificationChannel(NotificationChannel.APP)
                .build();

        when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

        notificationService.sendNotification(
                userId,
                "Aviso",
                "Mensaje",
                NotificationType.GENERAL,
                null
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(emailSender, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void should_not_send_anything_when_channel_is_none() {
        Member member = Member.builder()
                .id(userId)
                .email("test@example.com")
                .notificationChannel(NotificationChannel.NONE)
                .build();

        when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

        notificationService.sendNotification(
                userId,
                "Aviso",
                "Mensaje",
                NotificationType.GENERAL,
                null
        );

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void should_notify_match_scheduled_to_participants() {
        UUID tournamentId = UUID.randomUUID();
        UUID ins1 = UUID.randomUUID();
        UUID ins2 = UUID.randomUUID();
        UUID person1 = UUID.randomUUID();

        InscriptionEntity inscription1 = mock(InscriptionEntity.class);
        ParticipantEntity participant = mock(ParticipantEntity.class);
        PersonEntity personEntity = mock(PersonEntity.class);

        when(personEntity.getId()).thenReturn(person1);
        when(participant.getIndividualPerson()).thenReturn(personEntity);
        when(inscription1.getParticipant()).thenReturn(participant);
        when(jpaInscriptionRepository.findDetailedById(ins1)).thenReturn(Optional.of(inscription1));

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(userId);
        when(jpaMemberRepository.findByPersonId(person1)).thenReturn(Optional.of(memberEntity));

        Member member1 = Member.builder()
                .id(userId)
                .email("player1@example.com")
                .notificationChannel(NotificationChannel.BOTH)
                .build();
        when(memberRepository.findById(userId)).thenReturn(Optional.of(member1));

        notificationService.notifyMatchScheduled(
                tournamentId,
                "Torneo Primavera",
                ins1,
                ins2,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "Pista Central"
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(emailSender).sendNotificationEmail(eq("player1@example.com"), contains("Torneo Primavera"), contains("Pista Central"));
    }
}
