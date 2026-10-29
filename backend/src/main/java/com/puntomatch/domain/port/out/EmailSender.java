package com.puntomatch.domain.port.out;

public interface EmailSender {
    void sendEmailConfirmation(String email, String confirmationUrl);
}
