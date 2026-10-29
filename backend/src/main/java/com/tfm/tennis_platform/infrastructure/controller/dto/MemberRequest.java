package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;

public record MemberRequest(
    String email,
    String username,
    String password,
    String gender,
    String tier
) {}
