package com.tfm.tennis_platform.infrastructure.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class SecurityIT extends IntegrationTestBase {

    @Test
    void public_endpoints_accessible_without_token() throws Exception {
        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rankings/professionals"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/age-categories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/refs/nationalities"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/calendar/tournaments"))
                .andExpect(status().isOk());
    }

    @Test
    void auth_endpoints_accessible_without_token() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", "security" + UUID.randomUUID() + "@test.com");
        body.put("password", "Password123!");
        body.put("name", "Security User");
        body.put("role", "PLAYER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>())))
                .andExpect(status().isNoContent());
    }

    @Test
    void protected_endpoints_return_401_without_token() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/custom-age-categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/custom-age-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalid_token_returns_401_with_invalid_token_code() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer completely-invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void malformed_authorization_header_returns_401() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuator_health_accessible_without_auth_check() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 200 || status == 503,
                "Actuator health should return 200 or 503 (DB dependent), not require auth");
    }

    @Test
    void tournament_get_by_id_public_accessible() throws Exception {
        String fakeId = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get("/api/tournaments/" + fakeId))
                .andExpect(status().isNotFound());
    }
}
