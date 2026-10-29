package com.tfm.tennis_platform.infrastructure.controller.dto;

public record ConsentRequest(
        boolean accepted,
        String privacyPolicyVersion
) {
}
