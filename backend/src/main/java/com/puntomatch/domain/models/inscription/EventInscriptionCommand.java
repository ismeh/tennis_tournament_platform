package com.puntomatch.domain.models.inscription;

import java.util.UUID;

public record EventInscriptionCommand(
        Integer categoryId,
        UUID partnerId
) {
}
