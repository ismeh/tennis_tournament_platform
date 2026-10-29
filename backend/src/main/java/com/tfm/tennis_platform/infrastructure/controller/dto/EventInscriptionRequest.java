package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record EventInscriptionRequest(
        Integer categoryId,
        UUID partnerId
) {
}
