package com.tfm.tennis_platform.infrastructure.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class TournamentControllerIT extends IntegrationTestBase {

    private static final String TOURNAMENTS_URL = "/api/tournaments";
    private static final String AUTH_URL = "/api/auth";

    private String registerAndLogin(String email) throws Exception {
        Map<String, Object> registerBody = new HashMap<>();
        registerBody.put("email", email);
        registerBody.put("password", "Password123!");
        registerBody.put("name", "Test Organizer");
        registerBody.put("role", "ORGANIZER");
        registerBody.put("privacyPolicyAccepted", true);

        mockMvc.perform(post(AUTH_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", email);
        loginBody.put("password", "Password123!");

        MvcResult result = mockMvc.perform(post(AUTH_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("accessToken").asText();
    }

    private String uniqueEmail() {
        return "org" + UUID.randomUUID() + "@test.com";
    }

    @Test
    void get_all_tournaments_returns_empty_list() throws Exception {
        mockMvc.perform(get(TOURNAMENTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void create_tournament_unauthenticated_returns_401() throws Exception {
        Map<String, Object> tournamentBody = createTournamentPayload();

        mockMvc.perform(post(TOURNAMENTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tournamentBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_tournament_authenticated_returns_201() throws Exception {
        String token = registerAndLogin(uniqueEmail());
        Map<String, Object> tournamentBody = createTournamentPayload();

        mockMvc.perform(post(TOURNAMENTS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tournamentBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.formalName").value("Test Tournament 2026"))
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    void get_tournament_by_id_after_creation() throws Exception {
        String token = registerAndLogin(uniqueEmail());
        Map<String, Object> tournamentBody = createTournamentPayload();

        MvcResult createResult = mockMvc.perform(post(TOURNAMENTS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tournamentBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String tournamentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get(TOURNAMENTS_URL + "/" + tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tournamentId))
                .andExpect(jsonPath("$.formalName").value("Test Tournament 2026"));
    }

    @Test
    void get_nonexistent_tournament_returns_404() throws Exception {
        String fakeId = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get(TOURNAMENTS_URL + "/" + fakeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_tournament_status_unauthenticated_returns_401() throws Exception {
        String statusBody = objectMapper.writeValueAsString(
                new HashMap<>() {{ put("status", "INSCRIPTION"); }}
        );

        mockMvc.perform(patch(TOURNAMENTS_URL + "/00000000-0000-0000-0000-000000000000/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_court_for_tournament() throws Exception {
        String token = registerAndLogin(uniqueEmail());
        Map<String, Object> tournamentBody = createTournamentPayload();

        MvcResult createResult = mockMvc.perform(post(TOURNAMENTS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tournamentBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String tournamentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        Map<String, String> courtBody = new HashMap<>();
        courtBody.put("name", "Court 1");

        mockMvc.perform(post(TOURNAMENTS_URL + "/" + tournamentId + "/courts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courtBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Court 1"));
    }

    @Test
    void get_courts_for_tournament() throws Exception {
        String token = registerAndLogin(uniqueEmail());
        Map<String, Object> tournamentBody = createTournamentPayload();

        MvcResult createResult = mockMvc.perform(post(TOURNAMENTS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tournamentBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String tournamentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        Map<String, String> courtBody = new HashMap<>();
        courtBody.put("name", "Court A");

        mockMvc.perform(post(TOURNAMENTS_URL + "/" + tournamentId + "/courts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courtBody)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(TOURNAMENTS_URL + "/" + tournamentId + "/courts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.name == 'Court A')]").exists());
    }

    private Map<String, Object> createTournamentPayload() {
        Map<String, Object> body = new HashMap<>();
        body.put("formalName", "Test Tournament 2026");
        body.put("playStartDate", "2026-07-01");
        body.put("playEndDate", "2026-07-05");
        body.put("tournamentStartTime", "09:00:00");
        body.put("inscriptionStartDate", "2026-06-01");
        body.put("inscriptionEndDate", "2026-06-25");
        body.put("surfaceCategory", "CLAY");
        body.put("maxPlayers", 32);
        body.put("location", "Madrid, Spain");
        body.put("locationLatitude", 40.4168);
        body.put("locationLongitude", -3.7038);
        body.put("courtCount", 2);
        return body;
    }
}
