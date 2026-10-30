package com.tfm.tennis_platform.domain.port.out;

public interface EmailSender {
    void sendEmailConfirmation(String email, String confirmationUrl);
    void sendNotificationEmail(String toEmail, String subject, String messageText);
}
