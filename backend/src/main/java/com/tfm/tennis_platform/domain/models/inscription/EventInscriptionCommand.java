package com.tfm.tennis_platform.domain.models.inscription;

import java.util.UUID;

public record EventInscriptionCommand(
        Integer categoryId,
        UUID partnerId
) {
}
