package com.tfm.tennis_platform.domain.models.enums;

public enum NotificationChannel {
    APP,
    EMAIL,
    BOTH,
    NONE;

    public boolean includesApp() {
        return this == APP || this == BOTH;
    }

    public boolean includesEmail() {
        return this == EMAIL || this == BOTH;
    }
}
