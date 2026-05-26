package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesEmailConfirmationContract() throws Exception {
        RegisterResponse response = new RegisterResponse(true, "Cuenta creada. Revisa tu correo para confirmar el email.");

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"emailVerificationRequired\":true"));
        assertTrue(json.contains("\"message\":\"Cuenta creada. Revisa tu correo para confirmar el email.\""));
    }
}
