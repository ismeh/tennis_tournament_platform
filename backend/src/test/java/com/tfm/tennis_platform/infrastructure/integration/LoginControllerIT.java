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

class LoginControllerIT extends IntegrationTestBase {

    private static final String BASE_URL = "/api/auth";
    private static final String TEST_PASSWORD = "Password123!";

    private String uniqueEmail() {
        return "login" + UUID.randomUUID() + "@test.com";
    }

    @Test
    void register_and_login_full_flow() throws Exception {
        String email = uniqueEmail();
        String registerBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", email);
                    put("password", TEST_PASSWORD);
                    put("name", "Test Player");
                    put("role", "PLAYER");
                }}
        );

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerificationRequired").value(false))
                .andExpect(jsonPath("$.message", notNullValue()));

        String loginBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", email);
                    put("password", TEST_PASSWORD);
                }}
        );

        MvcResult loginResult = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();

        mockMvc.perform(get(BASE_URL + "/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.tier").value("FREE"));

        String refreshBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("refreshToken", refreshToken);
                }}
        );

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));

        String logoutBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("refreshToken", refreshToken);
                }}
        );

        mockMvc.perform(post(BASE_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());
    }

    @Test
    void login_with_wrong_password_returns_401() throws Exception {
        String email = uniqueEmail();
        String registerBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", email);
                    put("password", TEST_PASSWORD);
                    put("name", "Test Player");
                    put("role", "PLAYER");
                }}
        );

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", email);
                    put("password", "WrongPassword123!");
                }}
        );

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void login_with_nonexistent_user_returns_401() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", "nonexistent" + uniqueEmail());
                    put("password", TEST_PASSWORD);
                }}
        );

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicate_email_returns_409() throws Exception {
        String email = uniqueEmail();
        String registerBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", email);
                    put("password", TEST_PASSWORD);
                    put("name", "Test Player");
                    put("role", "PLAYER");
                }}
        );

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict());
    }

    @Test
    void register_with_invalid_email_returns_400() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("email", "not-an-email");
                    put("password", TEST_PASSWORD);
                    put("name", "Test Player");
                    put("role", "PLAYER");
                }}
        );

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_profile_without_token_returns_401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void get_profile_with_invalid_token_returns_401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/profile")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void refresh_with_invalid_token_returns_401() throws Exception {
        String refreshBody = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("refreshToken", "invalid.refresh.token");
                }}
        );

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirm_email_when_disabled_returns_message() throws Exception {
        mockMvc.perform(get(BASE_URL + "/confirm-email")
                        .param("token", "some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerificationRequired").value(false));
    }
}
