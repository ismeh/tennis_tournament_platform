package com.tfm.tennis_platform.infrastructure.controller.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
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
                .andExpect(status().isCreated())
                .andDo(document("register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("email").description("User email address"),
                                fieldWithPath("password").description("User password"),
                                fieldWithPath("name").description("User display name"),
                                fieldWithPath("role").description("User role (PLAYER, ORGANIZER)"),
                                fieldWithPath("privacyPolicyAccepted").description("Whether user accepts the privacy policy")
                        ),
                        responseFields(
                                fieldWithPath("emailVerificationRequired").description("Whether email verification is needed"),
                                fieldWithPath("message").description("Registration status message")
                        )
                ));
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
                .andDo(document("login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("email").description("User email address"),
                                fieldWithPath("password").description("User password")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("JWT access token"),
                                fieldWithPath("refreshToken").description("JWT refresh token"),
                                fieldWithPath("role").description("User role")
                        )
                ));
    }

    @Test
    void get_tournaments_endpoint_documentation() throws Exception {
        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk())
                .andDo(document("get-tournaments",
                        preprocessResponse(prettyPrint())
                ));
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
                .andDo(document("login-error",
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("code").description("Error code"),
                                fieldWithPath("message").description("Error message"),
                                fieldWithPath("status").description("HTTP status code"),
                                fieldWithPath("timestamp").description("Error timestamp"),
                                fieldWithPath("path").description("Request path")
                        )
                ));
    }
}
