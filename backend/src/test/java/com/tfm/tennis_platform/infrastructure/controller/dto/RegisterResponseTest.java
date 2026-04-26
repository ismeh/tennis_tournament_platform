package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAccessTokenUsingTheSharedAuthContract() throws Exception {
        RegisterResponse response = new RegisterResponse("registered-token", "refresh-token");

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"accessToken\":\"registered-token\""));
        assertTrue(json.contains("\"refreshToken\":\"refresh-token\""));
    }
}