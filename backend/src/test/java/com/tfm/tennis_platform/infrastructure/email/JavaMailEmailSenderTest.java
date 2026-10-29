package com.tfm.tennis_platform.infrastructure.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JavaMailEmailSender")
class JavaMailEmailSenderTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender javaMailSender;

    private EmailProperties buildProperties(boolean enabled) {
        return new EmailProperties(false, enabled, "no-reply@tennis.com", "https://tennis.com/confirm", 30);
    }

    @Test
    @DisplayName("Should not call mailSenderProvider when email is disabled")
    void shouldSkipWhenDisabled() {
        JavaMailEmailSender senderWithProps = new JavaMailEmailSender(mailSenderProvider, buildProperties(false));

        senderWithProps.sendEmailConfirmation("user@test.com", "https://tennis.com/confirm/abc");

        verifyNoInteractions(mailSenderProvider);
    }

    @Test
    @DisplayName("Should not throw when email is enabled but no sender is available")
    void shouldNotThrowWhenNoSenderAvailable() {
        EmailProperties properties = buildProperties(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        JavaMailEmailSender senderWithProps = new JavaMailEmailSender(mailSenderProvider, properties);

        assertThatNoException().isThrownBy(
                () -> senderWithProps.sendEmailConfirmation("user@test.com", "https://tennis.com/confirm/abc")
        );

        verify(mailSenderProvider).getIfAvailable();
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("Should send mail with correct fields when sender is available")
    void shouldSendMailWithCorrectFields() {
        EmailProperties properties = buildProperties(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        JavaMailEmailSender senderWithProps = new JavaMailEmailSender(mailSenderProvider, properties);
        senderWithProps.sendEmailConfirmation("user@test.com", "https://tennis.com/confirm/abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@tennis.com");
        assertThat(message.getTo()).containsExactly("user@test.com");
        assertThat(message.getSubject()).isEqualTo("Confirma tu email en Tennis Platform");
        assertThat(message.getText()).contains("https://tennis.com/confirm/abc");
    }

    @Test
    @DisplayName("Should catch MailException when sending fails")
    void shouldCatchMailException() {
        EmailProperties properties = buildProperties(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("SMTP unavailable")).when(javaMailSender).send(any(SimpleMailMessage.class));

        JavaMailEmailSender senderWithProps = new JavaMailEmailSender(mailSenderProvider, properties);

        assertThatNoException().isThrownBy(
                () -> senderWithProps.sendEmailConfirmation("user@test.com", "https://tennis.com/confirm/abc")
        );

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }
}
