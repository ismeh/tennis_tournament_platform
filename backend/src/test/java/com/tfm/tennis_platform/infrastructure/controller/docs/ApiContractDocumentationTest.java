package com.tfm.tennis_platform.infrastructure.controller.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiContractDocumentationTest extends RestDocsTestBase {

    @Test
    void register_endpoint_documentation() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("email", "docs" + UUID.randomUUID() + "@test.com");
        body.put("password", "Password123!");
        body.put("name", "New User");
        body.put("role", "PLAYER");
        body.put("privacyPolicyAccepted", true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void login_endpoint_documentation() throws Exception {
        String email = "logindocs" + UUID.randomUUID() + "@test.com";
        Map<String, Object> registerBody = new HashMap<>();
        registerBody.put("email", email);
        registerBody.put("password", "Password123!");
        registerBody.put("name", "Login Docs User");
        registerBody.put("role", "PLAYER");
        registerBody.put("privacyPolicyAccepted", true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", email);
        loginBody.put("password", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void get_tournaments_endpoint_documentation() throws Exception {
        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk());
    }

    @Test
    void login_error_response_documentation() throws Exception {
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", "nonexistent" + UUID.randomUUID() + "@test.com");
        loginBody.put("password", "WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
