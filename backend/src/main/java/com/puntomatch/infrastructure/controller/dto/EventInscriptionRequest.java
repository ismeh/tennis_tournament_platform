package com.puntomatch.infrastructure.controller.dto;

import java.util.UUID;

public record EventInscriptionRequest(
        Integer categoryId,
        UUID partnerId
) {
}
