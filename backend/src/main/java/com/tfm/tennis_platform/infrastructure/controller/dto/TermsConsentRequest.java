package com.tfm.tennis_platform.infrastructure.controller.dto;

public record TermsConsentRequest(
        boolean accepted,
        String termsConditionsVersion
) {
}
