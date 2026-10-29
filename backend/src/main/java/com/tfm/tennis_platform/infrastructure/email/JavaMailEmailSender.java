package com.tfm.tennis_platform.infrastructure.email;

import com.tfm.tennis_platform.domain.port.out.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailSender implements EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailProperties properties;

    @Override
    public void sendEmailConfirmation(String email, String confirmationUrl) {
        if (!properties.enabled()) {
            log.info("Email confirmation link for {}: {}", email, confirmationUrl);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email confirmation is enabled but JavaMailSender is not configured. Link for {}: {}", email, confirmationUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject("Confirma tu email en Tennis Platform");
        message.setText("""
                Hola,

                Confirma tu email para activar tu cuenta:
                %s

                Si no has creado esta cuenta, ignora este mensaje.
                """.formatted(confirmationUrl));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn(
                    "Email confirmation could not be sent. Link for {}: {}",
                    email,
                    confirmationUrl,
                    exception
            );
        }
    }
}
