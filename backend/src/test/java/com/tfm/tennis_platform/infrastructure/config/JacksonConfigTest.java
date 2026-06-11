package com.tfm.tennis_platform.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.tennis_platform.infrastructure.controller.dto.ErrorResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonConfigTest {

    @Test
    void shouldSerializeErrorResponseTimestampAsIsoDateTime() throws Exception {
        ObjectMapper objectMapper = new JacksonConfig().objectMapper();

        String json = objectMapper.writeValueAsString(new ErrorResponse(
                "AUTHENTICATION_REQUIRED",
                "Inicia sesión para continuar.",
                401,
                LocalDateTime.of(2026, 6, 4, 10, 15, 30),
                "/api/tournaments"
        ));

        assertTrue(json.contains("\"timestamp\":\"2026-06-04T10:15:30\""));
    }
}
