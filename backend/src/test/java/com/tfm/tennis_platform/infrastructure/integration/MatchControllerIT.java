package com.tfm.tennis_platform.infrastructure.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchControllerIT extends IntegrationTestBase {

    private static final String MATCHES_URL = "/api/matches";
    private static final String AUTH_URL = "/api/auth";

    @Test
    void get_matches_for_tournament_returns_empty_list() throws Exception {
        String token = registerAndGetToken();
        UUID fakeTournamentId = UUID.randomUUID();

        mockMvc.perform(get(MATCHES_URL + "/tournament/" + fakeTournamentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void get_matches_with_invalid_token_returns_401() throws Exception {
        UUID fakeTournamentId = UUID.randomUUID();

        mockMvc.perform(get(MATCHES_URL + "/tournament/" + fakeTournamentId)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_match_without_auth_returns_401() throws Exception {
        UUID fakeMatchId = UUID.randomUUID();

        Map<String, Object> matchBody = new HashMap<>();
        matchBody.put("id", fakeMatchId.toString());

        mockMvc.perform(put(MATCHES_URL + "/" + fakeMatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_match_with_invalid_token_returns_401() throws Exception {
        UUID fakeMatchId = UUID.randomUUID();

        Map<String, Object> matchBody = new HashMap<>();
        matchBody.put("id", fakeMatchId.toString());

        mockMvc.perform(put(MATCHES_URL + "/" + fakeMatchId)
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void get_matches_for_nonexistent_tournament_returns_empty() throws Exception {
        String token = registerAndGetToken();
        UUID fakeTournamentId = UUID.randomUUID();

        mockMvc.perform(get(MATCHES_URL + "/tournament/" + fakeTournamentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    private String registerAndGetToken() throws Exception {
        String email = "matchplayer" + UUID.randomUUID() + "@test.com";
        Map<String, String> registerBody = new HashMap<>();
        registerBody.put("email", email);
        registerBody.put("password", "Password123!");
        registerBody.put("name", "Match Player");
        registerBody.put("role", "ORGANIZER");

        mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", email);
        loginBody.put("password", "Password123!");

        var result = mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
