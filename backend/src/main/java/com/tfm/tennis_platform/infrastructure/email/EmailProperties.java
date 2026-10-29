package com.tfm.tennis_platform.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.email.confirmation")
public record EmailProperties(
        boolean required,
        boolean enabled,
        String from,
        String frontendConfirmationUrl,
        int tokenExpirationMinutes
) {
}
