package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LoginRequest(
        @JsonAlias("username") String email,
        String password
) {
}
