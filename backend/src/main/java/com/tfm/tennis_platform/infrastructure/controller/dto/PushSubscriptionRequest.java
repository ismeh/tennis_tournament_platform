package com.tfm.tennis_platform.infrastructure.controller.dto;

public record PushSubscriptionRequest(String endpoint, PushSubscriptionKeys keys, String userAgent) {
}
