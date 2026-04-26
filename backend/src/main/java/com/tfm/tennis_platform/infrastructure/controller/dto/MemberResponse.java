package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
    UUID id,
    String email,
    String username,
    String gender,
    String tier,
    LocalDateTime registeredAt
) {}
